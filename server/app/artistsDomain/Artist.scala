package artistsDomain

import java.util.UUID
import javax.inject.Inject

import application.ThereIsNoArtistForThisFacebookIdException
import database.MyPostgresDriver.api._
import database.{EventArtistRelation, MyDBTableDefinitions, MyPostgresDriver, UserArtistRelation}
import genresDomain.{GenreMethods, GenreWithWeight}
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services._
import tracksDomain._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.matching.Regex


case class Artist(id: Option[Long] = None,
                  facebookId: Option[String] = None,
                  name: String,
                  imagePath: Option[String] = None,
                  description: Option[String] = None,
                  facebookUrl: String,
                  websites: Set[String] = Set.empty,
                  hasTracks: Boolean = false,
                  likes: Option[Int] = None,
                  country: Option[String] = None)

case class ArtistWithWeightedGenres(artist: Artist, genres: Seq[GenreWithWeight] = Seq.empty)

case class PatternAndArtist(searchPattern: String, artistWithWeightedGenres: ArtistWithWeightedGenres)


class ArtistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                       val genreMethods: GenreMethods,
                       val searchSoundCloudTracks: SearchSoundCloudTracks,
                       val searchYoutubeTracks: SearchYoutubeTracks,
                       val trackMethods: TrackMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with SoundCloudHelper
    with FollowService
    with MyDBTableDefinitions
    with artistsAndOptionalGenresToArtistsWithWeightedGenresTrait
    with Utilities
    with LoggerHelper {


  def findAll: Future[Vector[Artist]] = db.run(artists.result) map (_.toVector)

  def findSinceOffset(numberToReturn: Int, offset: Long): Future[Seq[ArtistWithWeightedGenres]] = {
    val query = for {
     artist <- artists.drop(offset).take(numberToReturn) joinLeft
       (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
    } yield artist

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.toVector)
  }

  def delete(id: Long): Future[Int] = db.run(artists.filter(_.id === id).delete)

  def findAllByEvent(eventId: Long): Future[Seq[ArtistWithWeightedGenres]] = {
    val query = for {
      e <- events if e.id === eventId
      eventArtist <- eventsArtists if eventArtist.eventId === e.id
      artist <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
      if artist._1.id === eventArtist.artistId
    } yield artist

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.toVector)
  }

  def findAllByGenre(genreName: String, offset: Int, numberToReturn: Int): Future[Seq[ArtistWithWeightedGenres]] = {
    val lowerCaseGenre = genreName.toLowerCase

    val artistsQuery = for {
      genre <- genres.filter(_.name === lowerCaseGenre)
      artistGenre <- artistsGenres.filter(_.genreId === genre.id)
      artist <- artists.filter(_.id === artistGenre.artistId) map (_.id)

    } yield artist

    val artistsIdFromDB = artistsQuery.drop(offset).take(numberToReturn)

    val artistWithGenreQuery = for {
      artist <- artists.filter(_.id in artistsIdFromDB) joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
    } yield artist

    db.run(artistWithGenreQuery.result) map { seqArtistAndOptionalGenre =>
      artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.toVector)
  }

  def find(id: Long): Future[Option[ArtistWithWeightedGenres]] = {
    val query = for {
      artist <- artists.filter(_.id === id) joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
    } yield artist

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.headOption)
  }

  def findByFacebookUrl(facebookUrl: String): Future[Option[ArtistWithWeightedGenres]] = {
    val query = for {
      artist <- artists.filter(_.facebookUrl === facebookUrl) joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
    } yield artist

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.headOption)
  }

  def findAllContaining(pattern: String): Future[Seq[ArtistWithWeightedGenres]] = {
    val lowercasePattern = pattern.toLowerCase

    val query = for {
      artist <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)

      if artist._1.name.toLowerCase like s"%$lowercasePattern%"
    } yield artist

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
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
    val artistWithFormattedDescription = artist.copy(description = formatDescription(artist.description))

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
          Iteratee.foreach(tracks => trackMethods.saveSequence(tracks))
      }
    }
  }

  def getArtistTracks(patternAndArtist: PatternAndArtist): Enumerator[Set[Track]] = {
    val soundCloudTracksEnumerator = Enumerator.flatten(
      searchSoundCloudTracks.getSoundCloudTracksForArtist(patternAndArtist.artistWithWeightedGenres.artist).map { soundCloudTracks =>
        soundCloudTracks.headOption match {
          case Some(track) =>
            addSoundCloudUrlIfMissing(track, patternAndArtist.artistWithWeightedGenres.artist)
            addWebsitesFoundOnSoundCloud(track, patternAndArtist.artistWithWeightedGenres.artist)
          case None =>
        }

        Enumerator(soundCloudTracks.toSet).andThen(Enumerator.eof)
      })

    val youtubeTracksEnumerator =
      searchYoutubeTracks.getYoutubeTracksForArtist(patternAndArtist.artistWithWeightedGenres.artist, patternAndArtist.searchPattern).andThen(Enumerator.eof)

    val youtubeTracksFromChannel = Enumerator.flatten(
      searchYoutubeTracks.getYoutubeTracksByChannel(patternAndArtist.artistWithWeightedGenres.artist) map { track =>
        Enumerator(track).andThen(Enumerator.eof)
      })

    val youtubeTracksFromYoutubeUser = Enumerator.flatten(
      searchYoutubeTracks.getYoutubeTracksByYoutubeUser(patternAndArtist.artistWithWeightedGenres.artist) map { track =>
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
      val refactoredRedirectUrl = removeUselessInSoundCloudWebsite(normalizeUrl(redirectUrl))
      if (!artist.websites.contains(refactoredRedirectUrl) && artist.id.nonEmpty)
        addWebsite(artist.id.get, refactoredRedirectUrl) recover {
          case e: Exception =>
            Logger.error("Artist.addSoundCloudUrlIfMissing: ", e)
            0
        }
      else
        Future(0)
  }

  def saveWithEventRelation(artist: ArtistWithWeightedGenres,
                            eventId: Long): Future[Artist] = save(artist) flatMap { savedArtist =>
    saveEventRelation(EventArtistRelation(eventId, savedArtist.id.getOrElse(0))) map {
      case 1 =>
        savedArtist
      case _ =>
        Logger.error(s"Artist.saveWithEventRelation: not exactly one row saved by Artist.saveEventRelation for " +
          s"artist $savedArtist and eventId $eventId")
        savedArtist
    }
  }

  def saveEventRelation(eventArtistRelation: EventArtistRelation): Future[Int] = 
    db.run(eventsArtists += eventArtistRelation) recover { case NonFatal(e) =>
      log(s"The relation $eventArtistRelation was not saved", e)
      0
    }

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

  def normalizeArtistName(artistName: String): String = normalizeString(artistName)
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

  def getEventuallyFacebookArtists(pattern: String): Future[Seq[ArtistWithWeightedGenres]] = WS
    .url("https://graph.facebook.com/" + facebookApiVersion + "/search")
    .withQueryString(
      "q" -> pattern,
      "type" -> "page",
      "limit" -> "400",
      "fields" -> facebookArtistFields,
      "access_token" -> facebookToken)
    .get()
    .flatMap(response => readFacebookArtists(response.json))
    .recover {
      case NonFatal(e: Exception) =>
        Logger.error(s"ArtistModel.getEventuallyFacebookArtists: for pattern $pattern\nMessage:\n" + e.getMessage)
        Seq.empty
    }

  def getEventuallyArtistsInEventTitle(eventName: String, websites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    val artistNames = splitArtistNamesInTitle(eventName)

    Future.sequence(artistNames.map(artistName => getArtistsForAnEvent(artistName, websites))).map { _.flatten }
  }

  def getArtistsForAnEvent(artistName: String, eventWebSites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    val websitesFound = eventWebSites collect {
      case website if website.contains("facebook") && website.contains(artistName.toLowerCase) => website
    }

    websitesFound match {
      case nonEmptyWebsites if nonEmptyWebsites.nonEmpty =>
        val artists = nonEmptyWebsites map { website =>
          normalizeFacebookUrl(website) match {
            case Some(url) =>
              getFacebookArtistByFacebookUrl(url) flatMap {
                case Some(artist) =>
                  Future(Option(artist))
                case _ =>
                  getFacebookArtistByFacebookUrl(url)
              }
            case _ =>
              Future(None)
          }
        }
        Future.sequence(artists).map { _.flatten.toVector }
      case _ =>
        getFacebookArtist(artistName, eventWebSites)
    }
  }

  def getFacebookArtist(artistName: String, eventWebSites: Set[String]): Future[Seq[ArtistWithWeightedGenres]] = {
    getEventuallyFacebookArtists(artistName).flatMap {
      case noArtist if noArtist.isEmpty && artistName.split("\\W+").size >= 2 =>
        val nestedEventuallyArtists = artistName.split("\\W+").toSeq.map { name =>
          getArtistsForAnEvent(name.trim, eventWebSites)
        }
        Future.sequence(nestedEventuallyArtists) map {
          _.toVector.flatten
        }
      case foundArtists =>
        Future {
          filterFacebookArtistsForEvent(foundArtists, artistName, eventWebSites)
        }
    }
  }

  def filterFacebookArtistsForEvent(artists: Seq[ArtistWithWeightedGenres], artistName: String, eventWebsites: Set[String])
  : Seq[ArtistWithWeightedGenres] = artists match {
    case onlyOneArtist: Seq[ArtistWithWeightedGenres] if onlyOneArtist.size == 1 &&
      onlyOneArtist.head.artist.name.toLowerCase == artistName.toLowerCase =>
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
      case website if website contains "facebook" => getFacebookArtistByFacebookUrl(website)

      case website if website contains "soundcloud" => getFacebookUrlBySoundCloudUrl(website) map { response =>
        readMaybeFacebookUrl(response.json)
      } flatMap {
        case None => Future(None)
        case Some(facebookUrl) => getFacebookArtistByFacebookUrl(facebookUrl)
      }

      case _ => Future(None)
    }
  ) map(_.flatten)

  def getFacebookUrlBySoundCloudUrl(soundCloudUrl: String): Future[WSResponse] = {
    val soundCloudName = soundCloudUrl.substring(soundCloudUrl.indexOf("/") + 1)

    WS.url("http://api.soundcloud.com/users/" + soundCloudName + "/web-profiles")
      .withQueryString("client_id" -> soundCloudClientId)
      .get()
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesResponse: JsValue): Option[String] = {
    val facebookUrlReads = (
      (__ \ "url").read[String] and
        (__ \ "service").read[String]
      )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
      urlService.collect { case (url: String, "facebook") => normalizeUrl(url) }
    }

    soundCloudWebProfilesResponse.asOpt[scala.Seq[String]](collectOnlyFacebookUrls) match {
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
        } else
          Option(alreadyNormalizedUrl)
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
      getFacebookArtist(normalizedFacebookUrl).flatMap(response => readFacebookArtist(response.json))
    case _ =>
      Future(None)
  }

  def getFacebookArtist(normalizedFacebookUrl: String): Future[WSResponse] = WS
    .url("https://graph.facebook.com/" + facebookApiVersion + "/" + normalizedFacebookUrl)
    .withQueryString(
      "fields" -> facebookArtistFields,
      "access_token" -> facebookToken)
    .get()

  val readArtist = (
   (__ \ "name").readNullable[String] and
     (__ \ "category").readNullable[String] and
     (__ \ "id").readNullable[String] and
     (__ \ "cover").readNullable[Option[String]](
       (__ \ "source").readNullable[String]
     ) and
     (__ \ "cover").readNullable[Int](
       (__ \ "offset_x").read[Int]
     ) and
     (__ \ "cover").readNullable[Int](
       (__ \ "offset_y").read[Int]
     ) and
     (__ \ "website").readNullable[String] and
     (__ \ "link").readNullable[String] and
     (__ \ "description").readNullable[String] and
     (__ \ "genre").readNullable[String] and
     (__ \ "likes").readNullable[Int] and
     (__ \ "location").readNullable[Option[String]](
       (__ \ "country").readNullable[String]
     )
   ).apply((name: Option[String], category: Option[String], id: Option[String], maybeCover: Option[Option[String]], maybeOffsetX: Option[Int],
            maybeOffsetY: Option[Int], websites: Option[String], link: Option[String], maybeDescription: Option[String],
            maybeGenre: Option[String], maybeLikes: Option[Int], maybeCountry: Option[Option[String]]) =>
   (name, id, category, maybeCover.flatten, maybeOffsetX, maybeOffsetY, websites, link, maybeDescription, maybeGenre,
     maybeLikes, maybeCountry))

  def readFacebookArtists(facebookJsonResponse: JsValue): Future[Seq[ArtistWithWeightedGenres]] = {
    val artistsRead: Reads[Future[Seq[ArtistWithWeightedGenres]]] = Reads.seq(readArtist) map { artists =>
      Future.sequence(artists.map(artistTupleToArtist).toVector) map (_.flatten)
    }

    (facebookJsonResponse \ "data")
      .asOpt[Future[Seq[ArtistWithWeightedGenres]]](artistsRead)
      .getOrElse(Future(Seq.empty))
  }

  def readFacebookArtist(facebookJsonResponse: JsValue): Future[Option[ArtistWithWeightedGenres]] = facebookJsonResponse
    .asOpt[(Option[String], Option[String], Option[String], Option[String], Option[Int], Option[Int], Option[String],
    Option[String], Option[String], Option[String], Option[Int], Option[Option[String]])](readArtist) match {
    case Some(artistTuple) =>
      artistTupleToArtist(artistTuple)
    case None =>
      Future(None)
  }

  def artistTupleToArtist(artist: (Option[String], Option[String], Option[String], Option[String], Option[Int],
    Option[Int], Option[String], Option[String], Option[String], Option[String], Option[Int], Option[Option[String]]))
  : Future[Option[ArtistWithWeightedGenres]] = {
    artist match {
      case (Some(name), Some(facebookId), Some(category), cover, maybeOffsetX, maybeOffsetY, websites, Some(link),
      maybeDescription, maybeGenre, maybeLikes, maybeCountry)
        if category.equalsIgnoreCase("Musician/Band") || category.equalsIgnoreCase("Artist") =>

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
    val facebookUrl = normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
    val eventuallyWebsitesSet: Future[Set[String]] = maybeWebsites match {
      case Some(websites) =>
        getNormalizedWebsitesInText(websites) map { websites =>
          websites.filterNot(_.contains("facebook.com")).filterNot(_ == "")
        } recover {
          case NonFatal(e) =>
            Logger.error("Artist.makeArtist:\nMessage:", e)
            Set.empty
        }
      case None =>
        Future(Set.empty)
    }

    val genres = maybeGenre match {
     case Some(genre) => genreMethods.genresStringToGenresSet(genre)
     case None => Set.empty
    }

    eventuallyWebsitesSet map { websitesSet =>
      ArtistWithWeightedGenres(Artist(None, Option(facebookId), name, cover, maybeDescription, facebookUrl, websitesSet),
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
      val normalizedUrl = removeUselessInSoundCloudWebsite(normalizeUrl(redirectUrl)).substring("soundcloud.com/".length)
      WS.url("http://api.soundcloud.com/users/" + normalizedUrl + "/web-profiles")
        .withQueryString("client_id" -> soundCloudClientId)
        .get()
        .map { soundCloudResponse =>
        readSoundCloudWebsites(soundCloudResponse) foreach { website =>
          val normalizedWebsite = normalizeUrl(website)
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
