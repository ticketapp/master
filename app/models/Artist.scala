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
import scala.util.{Failure, Success, Try}
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

case class ArtistWithWeightedGenres(artist: Artist, genres: Seq[GenreWithWeight])

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

  def findSinceOffset(numberToReturn: Int, offset: Int): Future[Seq[ArtistWithWeightedGenres]] = {
    val query = for {
      (artist, optionalArtistGenreAndGenre) <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
    } yield (artist, optionalArtistGenreAndGenre)

    db.run(query.drop(offset).take(numberToReturn).result) map { seqArtistAndOptionalGenre =>
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

  def findIdByName(name: String): Future[Option[Long]] =
    db.run(artists.filter(_.name === name).map(_.id).result.headOption)

  def findIdByFacebookId(facebookId: String): Future[Option[Long]] =
    db.run(artists.filter(_.facebookId === facebookId).map(_.id).result.headOption)

  def findIdByFacebookUrl(facebookUrl: String): Future[Option[Long]] =
    db.run(artists.filter(_.facebookUrl === facebookUrl).map(_.id).result.headOption)
  
  def save(artistWithWeightedGenres: ArtistWithWeightedGenres): Future[Artist] = {
    val artist = artistWithWeightedGenres.artist
    val genres = artistWithWeightedGenres.genres
    val genresWithoutWeight = genres map (_.genre)
    val artistWithFormattedDescription = artist.copy(description = utilities.formatDescription(artist.description))

    val genresWithOverGenres = genreMethods.findOverGenres(genresWithoutWeight) map { overGenres =>
      (genresWithoutWeight ++ overGenres).distinct
    }

    db.run((for {
      artistFound <- artists.filter(_.facebookUrl === artist.facebookUrl).result.headOption
      result <- artistFound.map(DBIO.successful).getOrElse(artists returning artists.map(_.id) += artistWithFormattedDescription)
    } yield result match {
      case a: Artist =>
        genresWithOverGenres map(_ map (genre => genreMethods.saveWithArtistRelation(genre = genre, artistId = a.id.get)))
        a
      case id: Long =>
        Logger.info("Artist.save: this artist is already saved")
        genresWithOverGenres map(_ map (genre => genreMethods.saveWithArtistRelation(genre = genre, artistId = id)))
        artistWithFormattedDescription.copy(id = Option(id))
    }).transactionally)
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
        val websites = artist.websites.mkString(",") match {
          case "" => normalizedUrl
          case nonEmptyString => nonEmptyString + "," + normalizedUrl
        }
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

  def saveWithEventRelation(artist: Artist, eventId: Long): Future[Artist] = save(ArtistWithWeightedGenres(artist, Vector.empty)) flatMap { artist =>
    saveEventRelation(EventArtistRelation(eventId, artist.id.getOrElse(0))) map {
      case 1 =>
        artist
      case _ =>
        Logger.error(s"Artist.saveWithEventRelation: not exactly one row saved by Artist.saveEventRelation for artist $artist and eventId $eventId")
        artist
    }
  }

  def saveEventRelation(eventArtistRelation: EventArtistRelation): Future[Int] =
    db.run(eventsArtists += eventArtistRelation)

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
     .map { resp =>
       readFacebookArtists(resp) match {
         case Success(success) =>
           success
         case Failure(e: Throwable) =>
           Logger.error("ArtistModel.getEventuallyFacebookArtists :", e)
           Seq.empty
       }
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

  def filterFacebookArtistsForEvent(artists: Seq[ArtistWithWeightedGenres], artistName: String, eventWebsites: Set[String]): Seq[ArtistWithWeightedGenres] =
    artists match {
    case onlyOneArtist: Seq[ArtistWithWeightedGenres] if onlyOneArtist.size == 1 &&
      onlyOneArtist.head.artist.name.toLowerCase == artistName =>
        onlyOneArtist
    case otherCase: Seq[ArtistWithWeightedGenres] =>
      val artists = otherCase.flatMap { artist: ArtistWithWeightedGenres =>
        if ((artist.artist.websites intersect eventWebsites).nonEmpty) Option(artist)
        else None
      }
      artists
  }

  def getFacebookArtistsByWebsites(websites: Set[String]): Future[Set[Option[ArtistWithWeightedGenres]]] = {
    Future.sequence(
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
    )
  }

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

  def normalizeFacebookUrl(facebookUrl: String): String = {
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
          } else if (normalizedUrl.indexOf("/") > -1) {
            Option(normalizedUrl.take(normalizedUrl.indexOf("/")))
          } else {
            Option(normalizedUrl)
          }
        } else {
          Option(alreadyNormalizedUrl)
        }
    }
    firstNormalization match {
      case Some(urlWithArguments) if urlWithArguments contains "?" =>
        urlWithArguments.slice(0, urlWithArguments.lastIndexOf("?"))
      case Some(urlWithoutArguments) =>
        urlWithoutArguments
    }
  }


  def getFacebookArtistByFacebookUrl(url: String): Future[Option[ArtistWithWeightedGenres]] = {
    WS.url("https://graph.facebook.com/"+ facebookApiVersion + "/" + normalizeFacebookUrl(url))
     .withQueryString(
       "fields" -> facebookArtistFields,
       "access_token" -> facebookToken)
     .get()
     .map { readFacebookArtist }
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

  def readFacebookArtists(facebookWSResponse: WSResponse): Try[Seq[ArtistWithWeightedGenres]] = Try {
    val collectOnlyArtistsWithCover: Reads[Seq[ArtistWithWeightedGenres]] = Reads.seq(readArtist).map { artists =>
      artists.collect {
       case (name, facebookId, category, Some(cover: String), maybeOffsetX, maybeOffsetY, websites, link,
       maybeDescription, maybeGenre, maybeLikes, maybeCountry)
         if category.equalsIgnoreCase("Musician/Band") | category.equalsIgnoreCase("Artist") =>
         makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), websites, link,
           maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten)
      }.toVector
    }
    (facebookWSResponse.json \ "data")
     .asOpt[Seq[ArtistWithWeightedGenres]](collectOnlyArtistsWithCover)
     .getOrElse(Seq.empty)
  }

  def splitArtistNamesInTitle(title: String): List[String] =
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "").map {
      _.toLowerCase.replace("live", "").replace("djset", "").replace("dj set", "").replace("set", "").trim()
    }

  def readFacebookArtist(facebookWSResponse: WSResponse): Option[ArtistWithWeightedGenres] = {
    facebookWSResponse.json
     .asOpt[(String, String, String, Option[String], Option[Int], Option[Int], Option[String], String,
     Option[String], Option[String], Option[Int], Option[Option[String]])](readArtist)
    match {
     case Some((name, facebookId, "Musician/Band", Some(cover: String), maybeOffsetX, maybeOffsetY, maybeWebsites,
     link, maybeDescription, maybeGenre, maybeLikes, maybeCountry)) =>
       Option(makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), maybeWebsites,
         link, maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten))
     case Some((name, facebookId, "Artist", Some(cover: String), maybeOffsetX, maybeOffsetY, maybeWebsites,
     link, maybeDescription, maybeGenre, maybeLikes, maybeCountry)) =>
       Option(makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), maybeWebsites,
         link, maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten))
     case _ => None
    }
  }

  def makeArtist(name: String, facebookId: String, cover: String, maybeWebsites: Option[String], link: String,
                maybeDescription: Option[String], maybeGenre: Option[String], maybeLikes: Option[Int],
                maybeCountry: Option[String]): ArtistWithWeightedGenres = {
    val facebookUrl = utilities.normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
    val websitesSet = utilities.getNormalizedWebsitesInText(maybeWebsites)
     .filterNot(_.contains("facebook.com"))
     .filterNot(_ == "")
    val description = utilities.formatDescription(maybeDescription)
    val genres = maybeGenre match {
     case Some(genre) => genreMethods.genresStringToGenresSet(genre)
     case None => Set.empty
    }
    ArtistWithWeightedGenres(Artist(None, Option(facebookId), name, Option(cover), description, facebookUrl, websitesSet),
    genres.toSeq.map{genre => GenreWithWeight(genre, 0) }.toVector)
  }

  def aggregateImageAndOffset(imgUrl: String, offsetX: Option[Int], offsetY: Option[Int]): String =
    imgUrl + """\""" + offsetX.getOrElse(0).toString + """\""" + offsetY.getOrElse(0).toString

  def addWebsitesFoundOnSoundCloud(track: Track, artist: Artist): Future[Seq[String]] = track.redirectUrl match {
    case None =>
      Future(Seq.empty)
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
  }
}
