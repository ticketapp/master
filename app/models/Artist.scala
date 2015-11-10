package models

import java.util.UUID
import javax.inject.Inject

import controllers.ThereIsNoArtistForThisFacebookIdException
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.matching.Regex


case class Artist(id: Option[Long],
                  facebookId: Option[String],
                  name: String,
                  imagePath: Option[String] = None,
                  description: Option[String] = None,
                  facebookUrl: String,
                  websites: Set[String] = Set.empty,
                  likes: Option[Int] = None,
                  country: Option[String] = None)

case class ArtistWithWeightedGenres(artist: Artist, genres: Seq[GenreWithWeight] = Seq.empty)

case class GenreWithWeight(genre: Genre, weight: Int = 1)

case class PatternAndArtist(searchPattern: String, artistWithWeightedGenre: ArtistWithWeightedGenres)


class ArtistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                       val genreMethods: GenreMethods,
                       val searchSoundCloudTracks: SearchSoundCloudTracks,
                       val searchYoutubeTracks: SearchYoutubeTracks,
                       val trackMethods: TrackMethods,
                       val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with SoundCloudHelper
    with FollowService
    with MyDBTableDefinitions {

  val facebookToken = utilities.facebookToken
  val soundCloudClientId = utilities.soundCloudClientId
  val facebookApiVersion = utilities.facebookApiVersion

  def findAll: Future[Vector[Artist]] = db.run(artists.result) map (_.toVector)

  def findSinceOffset(numberToReturn: Int, offset: Int): Future[Seq[ArtistWithWeightedGenres]] = {
    val query = for {
      (artist, optionalArtistGenreAndGenre) <- artists.drop(offset).take(numberToReturn) joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
    } yield (artist, optionalArtistGenreAndGenre)

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      ArtistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.toVector)
  }

  def ArtistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre: scala.Seq[(Artist, Option[(ArtistGenreRelation, Genre)])])
  : Iterable[ArtistWithWeightedGenres] = {
    val groupedByArtist = seqArtistAndOptionalGenre.groupBy(_._1)

    val artistsWithGenres = groupedByArtist map { tupleArtistSeqTupleArtistWithMaybeGenres =>
      (tupleArtistSeqTupleArtistWithMaybeGenres._1, tupleArtistSeqTupleArtistWithMaybeGenres._2 collect {
        case (_, Some((artistGenre, genre))) => GenreWithWeight(genre, artistGenre.weight)
      })
    }

    artistsWithGenres map (artistWithGenre => ArtistWithWeightedGenres(artistWithGenre._1, artistWithGenre._2.to[Seq]))
  }

  def delete(id: Long): Future[Int] = db.run(artists.filter(_.id === id).delete)

  def findAllByEvent(eventId: Long): Future[Seq[ArtistWithWeightedGenres]] = {
    val query = for {
      e <- events if e.id === eventId
      eventArtist <- eventsArtists
      (artist, optionalArtistGenreAndGenre) <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
      if artist.id === eventArtist.artistId
    } yield (artist, optionalArtistGenreAndGenre)

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      ArtistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.toVector)
  }
  
  def findAllByGenre(genreName: String, offset: Int, numberToReturn: Int): Future[Seq[Artist]] = {
    val query = for {
      genre <- genres if genre.name === genreName
      artistGenre <- artistsGenres if artistGenre.genreId === genre.id
      artist <- artists if artist.id === artistGenre.artistId
    } yield artist

    db.run(query.drop(offset).take(numberToReturn).result) map { _.toVector }
  }
  
  def find(id: Long): Future[Option[ArtistWithWeightedGenres]] = {
    val query = for {
      (artist, optionalArtistGenreAndGenre) <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
      if artist.id === id
    } yield (artist, optionalArtistGenreAndGenre)

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      ArtistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.headOption)
  }

  def findByFacebookUrl(facebookUrl: String): Future[Option[ArtistWithWeightedGenres]] = {
    val query = for {
      (artist, optionalArtistGenreAndGenre) <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
      if artist.facebookUrl === facebookUrl
    } yield (artist, optionalArtistGenreAndGenre)

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      ArtistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.headOption)
  }

  def findAllContaining(pattern: String): Future[Seq[ArtistWithWeightedGenres]] = {
    val lowercasePattern = pattern.toLowerCase

    val query = for {
      (artist, optionalArtistGenreAndGenre) <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
      if artist.name.toLowerCase like s"%$lowercasePattern%"
    } yield (artist, optionalArtistGenreAndGenre)

    db.run(query.take(20).result) map { seqArtistAndOptionalGenre =>
      ArtistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.toVector)
  }

  def findIdByFacebookId(facebookId: String): Future[Option[Long]] =
    db.run(artists.filter(_.facebookId === facebookId).map(_.id).result.headOption)

  def findIdByFacebookUrl(facebookUrl: String): Future[Option[Long]] =
    db.run(artists.filter(_.facebookUrl === facebookUrl).map(_.id).result.headOption)
  
  def save(artistWithWeightedGenres: ArtistWithWeightedGenres): Future[Artist] = formatArtist(artistWithWeightedGenres) flatMap {
    formattedArtist =>
      db.run((for {
        artistFound <- artists.filter(_.facebookUrl === formattedArtist.artist.facebookUrl).result.headOption
        result <- artistFound.map(DBIO.successful).getOrElse(artists returning artists.map(_.id) += formattedArtist.artist)
      } yield result match {
        case artist: Artist =>
          Logger.info("Artist.save: this artist is already saved")
          formattedArtist.genres map(genre => genreMethods.saveWithArtistRelation(genre = genre.genre, artistId = artist.id.get))
          artist
        case id: Long =>
          formattedArtist.genres map(genre => genreMethods.saveWithArtistRelation(genre = genre.genre, artistId = id))
          formattedArtist.artist.copy(id = Option(id))
      }).transactionally)
  }

  def saveOrReturnNoneIfDuplicate(artistWithWeightedGenres: ArtistWithWeightedGenres): Future[Option[Artist]] =
    formatArtist(artistWithWeightedGenres) flatMap { formattedArtist =>

    db.run((for {
      artistFound <- artists.filter(_.facebookUrl === formattedArtist.artist.facebookUrl).result.headOption
      result <- artistFound.map(DBIO.successful).getOrElse(artists returning artists.map(_.id) += formattedArtist.artist)
    } yield result match {
        case artist: Artist =>
          Logger.info("Artist.save: this artist is already saved")
          None
        case id: Long =>
          formattedArtist.genres map(genre => genreMethods.saveWithArtistRelation(genre = genre.genre, artistId = id))
          Option(formattedArtist.artist.copy(id = Option(id)))
      }).transactionally)
  }

  def formatArtist(artistWithWeightedGenres: ArtistWithWeightedGenres): Future[ArtistWithWeightedGenres] = {
    val artist = artistWithWeightedGenres.artist
    val genres = artistWithWeightedGenres.genres
    val genresWithoutWeight = genres map (_.genre)
    val artistWithFormattedDescription = artist.copy(description = utilities.formatDescription(artist.description))

    val genresWithOverGenres = genreMethods.findOverGenres(genresWithoutWeight) map { overGenres =>
      (genresWithoutWeight ++ overGenres).distinct
    }

    genresWithOverGenres map { allGenres =>
      ArtistWithWeightedGenres(artistWithFormattedDescription, allGenres map (g => GenreWithWeight(g)))
    }
  }

  def update(artist: Artist): Future[Int] = db.run(artists.filter(_.id === artist.id).update(artist))

  def saveArtistsAndTheirTracks(artists: Seq[Artist]): Unit = Future {
    artists.map { artist =>
      save(ArtistWithWeightedGenres(artist, Vector.empty)) map { artistSaved =>
        getArtistTracks(PatternAndArtist(artistSaved.name, ArtistWithWeightedGenres(artistSaved, Vector.empty))) |>>
          Iteratee.foreach(_ map trackMethods.save)
      }
    }
  }

  def getArtistTracks(patternAndArtist: PatternAndArtist): Enumerator[Set[Track]] = {
    val soundCloudTracksEnumerator = Enumerator.flatten(
      searchSoundCloudTracks.getSoundCloudTracksForArtist(patternAndArtist.artistWithWeightedGenre.artist).map { soundCloudTracks =>
        soundCloudTracks.headOption match {
          case Some(track) =>
            addSoundCloudUrlIfMissing(track, patternAndArtist.artistWithWeightedGenre.artist)
            addWebsitesFoundOnSoundCloud(track, patternAndArtist.artistWithWeightedGenre.artist)
          case None =>
        }
        Enumerator(soundCloudTracks.toSet).andThen(Enumerator.eof)
      })

    val youtubeTracksEnumerator =
      searchYoutubeTracks.getYoutubeTracksForArtist(patternAndArtist.artistWithWeightedGenre.artist, patternAndArtist.searchPattern).andThen(Enumerator.eof)

    val youtubeTracksFromChannel = Enumerator.flatten(
      searchYoutubeTracks.getYoutubeTracksByChannel(patternAndArtist.artistWithWeightedGenre.artist) map { track =>
        Enumerator(track).andThen(Enumerator.eof)
      })

    val youtubeTracksFromYoutubeUser = Enumerator.flatten(
      searchYoutubeTracks.getYoutubeTracksByYoutubeUser(patternAndArtist.artistWithWeightedGenre.artist) map { track =>
        Enumerator(track).andThen(Enumerator.eof)
      })

    Enumerator.interleave(soundCloudTracksEnumerator, youtubeTracksEnumerator, youtubeTracksFromChannel,
      youtubeTracksFromYoutubeUser).andThen(Enumerator.eof)
  }

  def addWebsite(artistId: Long, normalizedUrl: String): Future[Int] = {
    val query = artists.filter(_.id === artistId)

    db.run(query.result.headOption) flatMap {
      case Some(artist) =>
        val websites = (artist.websites + normalizedUrl).mkString(",")
        db.run(query.map(_.websites).update(Option(websites)))
      case None =>
        Logger.info("Artist.addWebsite: there is no artist with the id " + artistId)
        Future(0)
    }
  }

  def addSoundCloudUrlIfMissing(soundCloudTrack: Track, artist: Artist): Future[Int] = soundCloudTrack.redirectUrl match {
    case None =>
      Future(0)
    case Some(redirectUrl) =>
      val refactoredRedirectUrl = removeUselessInSoundCloudWebsite(utilities.normalizeUrl(redirectUrl))
      if (!artist.websites.contains(refactoredRedirectUrl) && artist.id.nonEmpty)
        addWebsite(artist.id.get, refactoredRedirectUrl) recover {
          case e: Exception =>
            Logger.error("Artist.addSoundCloudUrlIfMissing: ", e)
            0
        }
      else
        Future(0)
  }

  def saveWithEventRelation(artist: ArtistWithWeightedGenres, eventId: Long): Future[Artist] = save(artist) flatMap { savedArtist =>
    saveEventRelation(EventArtistRelation(eventId, savedArtist.id.getOrElse(0))) map {
      case 1 =>
        savedArtist
      case _ =>
        Logger.error(s"Artist.saveWithEventRelation: not exactly one row saved by Artist.saveEventRelation for artist $savedArtist and eventId $eventId")
        savedArtist
    }
  }

  def saveEventRelation(eventArtistRelation: EventArtistRelation): Future[Int] = 
    db.run(eventsArtists += eventArtistRelation)

  def saveEventRelations(eventArtistRelations: Seq[EventArtistRelation]): Future[Boolean] =
    db.run(eventsArtists ++= eventArtistRelations) map { _ =>
      true
    } recover {
      case e: Exception =>
        Logger.error("Artist.saveEventRelations: ", e)
        false
    }

  def deleteEventRelation(eventArtistRelation: EventArtistRelation): Future[Int] = db.run(
    eventsArtists
      .filter(artistFollowed =>
        artistFollowed.artistId === eventArtistRelation.artistId && artistFollowed.eventId === eventArtistRelation.eventId)
      .delete)

  def followByFacebookId(userId : UUID, facebookId: String): Future[Int] = findIdByFacebookId(facebookId) flatMap {
    case None =>
      Logger.error("Artist.followByFacebookId: ", ThereIsNoArtistForThisFacebookIdException("Artist.followByFacebookId"))
      Future { 0 }
    case Some(artistId) =>
      followByArtistId(UserArtistRelation(userId, artistId))
  }

  def normalizeArtistName(artistName: String): String = utilities.normalizeString(artistName)
    .toLowerCase
    .replaceAll("officiel", "")
    .replaceAll("fanpage", "")
    .replaceAll("official", "")
    .replaceAll("fb", "")
    .replaceAll("facebook", "")
    .replaceAll("page", "")
    .trim
    .replaceAll("""\s+""", " ")

  val facebookArtistFields = "name,cover{source,offset_x,offset_y},id,category,link,website,description,genre,location,likes"

  def getEventuallyFacebookArtists(pattern: String): Future[Seq[ArtistWithWeightedGenres]] = {
    WS.url("https://graph.facebook.com/" + facebookApiVersion + "/search")
     .withQueryString(
       "q" -> pattern,
       "type" -> "page",
       "limit" -> "400",
       "fields" -> facebookArtistFields,
       "access_token" -> facebookToken)
     .get()
     .flatMap { response =>
       readFacebookArtists(response.json)
     } recover {
      case t: Throwable =>
        Logger.error(s"ArtistModel.getEventuallyFacebookArtists: for pattern $pattern\nMessage:\n" + t.getMessage)
        Seq.empty
    }
  }

  def getEventuallyArtistsInEventTitle(eventName: String, websites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    val artistNames = splitArtistNamesInTitle(eventName)
    Future.sequence(
      artistNames.map { artistName => getArtistsForAnEvent(artistName, websites) }
    ).map { _.flatten }
  }

  def getArtistsForAnEvent(artistName: String, eventWebSites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    getEventuallyFacebookArtists(artistName).flatMap {
      case noArtist if noArtist.isEmpty && artistName.split("\\W+").size >= 2 =>
        val nestedEventuallyArtists = artistName.split("\\W+").toSeq.map { getArtistsForAnEvent(_, eventWebSites) }
        Future.sequence(nestedEventuallyArtists) map { _.toVector.flatten }
      case foundArtists =>
        Future { filterFacebookArtistsForEvent(foundArtists, artistName, eventWebSites)}
    }
  }

  def filterFacebookArtistsForEvent(artists: Seq[ArtistWithWeightedGenres], artistName: String, eventWebsites: Set[String])
  : Seq[ArtistWithWeightedGenres] = artists match {
    case onlyOneArtist: Seq[ArtistWithWeightedGenres] if onlyOneArtist.size == 1 &&
      onlyOneArtist.head.artist.name.toLowerCase == artistName =>
        onlyOneArtist

    case otherCase: Seq[ArtistWithWeightedGenres] =>
      val artists = otherCase.flatMap { artist: ArtistWithWeightedGenres =>
        if ((artist.artist.websites intersect eventWebsites).nonEmpty) Option(artist)
        else None
      }
      artists

    case _  =>
      artists
  }

  def getFacebookArtistsByWebsites(websites: Set[String]): Future[Set[ArtistWithWeightedGenres]] = Future.sequence(
    websites.map {
      case website if website contains "facebook" =>
        getFacebookArtistByFacebookUrl(website).map { maybeFacebookArtist => maybeFacebookArtist }
      case website if website contains "soundcloud" =>
        getMaybeFacebookUrlBySoundCloudUrl(website) flatMap {
          case None =>
            Future { None }
          case Some(facebookUrl) =>
            getFacebookArtistByFacebookUrl(facebookUrl).map { maybeFacebookArtist => maybeFacebookArtist }
        }
      case _ =>
        Future { None }
    }
  ) map(_.flatten)

  def getMaybeFacebookUrlBySoundCloudUrl(soundCloudUrl: String): Future[Option[String]] = {
    val soundCloudName = soundCloudUrl.substring(soundCloudUrl.indexOf("/") + 1)
    WS.url("http://api.soundcloud.com/users/" + soundCloudName + "/web-profiles")
      .withQueryString("client_id" -> utilities.soundCloudClientId)
      .get()
      .map { readMaybeFacebookUrl }
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesResponse: WSResponse): Option[String] = {
    val facebookUrlReads = (
      (__ \ "url").read[String] and
        (__ \ "service").read[String]
      )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
      urlService.collect { case (url: String, "facebook") => utilities.normalizeUrl(url) }
    }

    soundCloudWebProfilesResponse.json.asOpt[scala.Seq[String]](collectOnlyFacebookUrls) match {
      case Some(facebookUrls: Seq[String]) if facebookUrls.nonEmpty => Option(facebookUrls.head)
      case _ => None
    }
  }

  def normalizeFacebookUrl(facebookUrl: String): Option[String] = {
    val firstNormalization = facebookUrl.toLowerCase match {
      case urlWithProfile: String if urlWithProfile contains "profile.php?id=" =>
        Option(urlWithProfile.substring(urlWithProfile.lastIndexOf("=") + 1))

      case alreadyNormalizedUrl: String =>
        if (alreadyNormalizedUrl.indexOf("facebook.com/") > -1) {
          val normalizedUrl = alreadyNormalizedUrl.substring(alreadyNormalizedUrl.indexOf("facebook.com/") + 13)
          if (normalizedUrl.indexOf("pages/") > -1) {
            val idRegex = new Regex("/[0-9]+")
            idRegex.findAllIn(normalizedUrl).toSeq.headOption match {
              case Some(id) => Option(id.replace("/", ""))
              case None => None
            }
          } else if (normalizedUrl.indexOf("/") > -1) Option(normalizedUrl.take(normalizedUrl.indexOf("/")))
            else Option(normalizedUrl)
        } else Option(alreadyNormalizedUrl)
      case _ =>
        None
    }

    firstNormalization match {
      case Some(urlWithArguments) if urlWithArguments contains "?" =>
        Option(urlWithArguments.slice(0, urlWithArguments.lastIndexOf("?")))
      case Some(urlWithoutArguments) =>
        Option(urlWithoutArguments)
      case _ =>
        None
    }
  }

  def getFacebookArtistByFacebookUrl(url: String): Future[Option[ArtistWithWeightedGenres]] =  normalizeFacebookUrl(url) match {
    case Some(normalizedFacebookUrl) =>
      WS.url("https://graph.facebook.com/"+ facebookApiVersion + "/" + normalizeFacebookUrl(url))
       .withQueryString(
         "fields" -> facebookArtistFields,
         "access_token" -> facebookToken)
       .get()
       .flatMap(response => readFacebookArtist(response.json))
    case _ =>
      Future(None)
  }

  val readArtist = (
   (__ \ "name").read[String] and
     (__ \ "category").read[String] and
     (__ \ "id").read[String] and
     (__ \ "cover").readNullable[String](
       (__ \ "source").read[String]
     ) and
     (__ \ "cover").readNullable[Int](
       (__ \ "offset_x").read[Int]
     ) and
     (__ \ "cover").readNullable[Int](
       (__ \ "offset_y").read[Int]
     ) and
     (__ \ "website").readNullable[String] and
     (__ \ "link").read[String] and
     (__ \ "description").readNullable[String] and
     (__ \ "genre").readNullable[String] and
     (__ \ "likes").readNullable[Int] and
     (__ \ "location").readNullable[Option[String]](
       (__ \ "country").readNullable[String]
     )
   ).apply((name: String, category: String, id: String, maybeCover: Option[String], maybeOffsetX: Option[Int],
            maybeOffsetY: Option[Int], websites: Option[String], link: String, maybeDescription: Option[String],
            maybeGenre: Option[String], maybeLikes: Option[Int], maybeCountry: Option[Option[String]]) =>
   (name, id, category, maybeCover, maybeOffsetX, maybeOffsetY, websites, link, maybeDescription, maybeGenre,
     maybeLikes, maybeCountry))

  def readFacebookArtists(facebookJsonResponse: JsValue): Future[Seq[ArtistWithWeightedGenres]] = {
    val collectOnlyArtistsWithCover: Reads[Future[Seq[ArtistWithWeightedGenres]]] = Reads.seq(readArtist) map { artists =>
      Future.sequence(artists.map(artistTupleToArtist).toVector) map (_.flatten)
    }

    (facebookJsonResponse \ "data")
     .asOpt[Future[Seq[ArtistWithWeightedGenres]]](collectOnlyArtistsWithCover)
     .getOrElse(Future(Seq.empty))
  }

  def readFacebookArtist(facebookJsonResponse: JsValue): Future[Option[ArtistWithWeightedGenres]] = {
    facebookJsonResponse
     .asOpt[(String, String, String, Option[String], Option[Int], Option[Int], Option[String], String,
     Option[String], Option[String], Option[Int], Option[Option[String]])](readArtist)
    match {
      case Some(artistTuple) =>
        artistTupleToArtist(artistTuple)
      case None =>
        Future(None)
    }
  }

  def artistTupleToArtist(artist: (String, String, String, Option[String], Option[Int], Option[Int], Option[String], String,
    Option[String], Option[String], Option[Int], Option[Option[String]])): Future[Option[ArtistWithWeightedGenres]] = {
    artist match {
      case (name, facebookId, category, cover, maybeOffsetX, maybeOffsetY, websites, link,
      maybeDescription, maybeGenre, maybeLikes, maybeCountry)
        if category.equalsIgnoreCase("Musician/Band") | category.equalsIgnoreCase("Artist") =>

        makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), websites, link,
          maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten) map Option.apply

      case _ =>
        Future(None)
    }
  }

  def splitArtistNamesInTitle(title: String): List[String] =
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "").map {
      _.toLowerCase.replace("live", "").replace("djset", "").replace("dj set", "").replace("set", "").trim()
    }

  def makeArtist(name: String, facebookId: String, cover: Option[String], maybeWebsites: Option[String], link: String,
                maybeDescription: Option[String], maybeGenre: Option[String], maybeLikes: Option[Int],
                maybeCountry: Option[String]): Future[ArtistWithWeightedGenres] = {
    val facebookUrl = utilities.normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
    val eventuallyWebsitesSet: Future[Set[String]] = maybeWebsites match {
      case Some(websites) =>
        utilities.getNormalizedWebsitesInText(websites) map { websites =>
          websites.filterNot(_.contains("facebook.com")).filterNot(_ == "")
        }
      case None =>
        Future(Set.empty)
    }
    val description = utilities.formatDescription(maybeDescription)
    val genres = maybeGenre match {
     case Some(genre) => genreMethods.genresStringToGenresSet(genre)
     case None => Set.empty
    }

    eventuallyWebsitesSet map { websitesSet =>
      ArtistWithWeightedGenres(Artist(None, Option(facebookId), name, cover, description, facebookUrl, websitesSet),
        genres.toSeq.map{genre => GenreWithWeight(genre, 0) }.toVector)
    }
  }

  def aggregateImageAndOffset(maybeImgUrl: Option[String], offsetX: Option[Int], offsetY: Option[Int]): Option[String] = maybeImgUrl match {
    case Some(imgUrl) =>
      Option(imgUrl + """\""" + offsetX.getOrElse(0).toString + """\""" + offsetY.getOrElse(0).toString)
    case None =>
      None
  }

  def addWebsitesFoundOnSoundCloud(track: Track, artist: Artist): Future[Seq[String]] = track.redirectUrl match {
    case Some(redirectUrl) =>
      val normalizedUrl = removeUselessInSoundCloudWebsite(utilities.normalizeUrl(redirectUrl)).substring("soundcloud.com/".length)
      WS.url("http://api.soundcloud.com/users/" + normalizedUrl + "/web-profiles")
        .withQueryString("client_id" -> utilities.soundCloudClientId)
        .get()
        .map { soundCloudResponse =>
        readSoundCloudWebsites(soundCloudResponse) foreach { website =>
          val normalizedWebsite = utilities.normalizeUrl(website)
          if (!artist.websites.contains(normalizedWebsite) && normalizedWebsite.indexOf("facebook") == -1 && artist.id.nonEmpty)
            addWebsite(artist.id.get, normalizedWebsite) map {
              case res if res != 1 =>
                Logger.error("Artist.addWebsitesFoundOnSoundCloud: not exactly one row was updated by addWebsite for artist " +
                  artist + "for website " + normalizedWebsite)
            }
        }
        readSoundCloudWebsites(soundCloudResponse)
      }
    case _ =>
      Future(Seq.empty)
  }
}
