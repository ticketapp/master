package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import controllers.{ThereIsNoArtistForThisFacebookIdException, DAOException, WebServiceException}
import models.Genre._
import play.api.libs.iteratee.{Enumerator, Enumeratee, Iteratee}

import services.SearchSoundCloudTracks._
import services.SearchYoutubeTracks._
import services.{SearchSoundCloudTracks, Utilities}

import play.api.libs.json._
import play.api.Play.current
import java.util.{UUID, Date}
import play.api.libs.ws.WSResponse
import services.Utilities._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import scala.util.{Try, Failure, Success}
import services.Utilities.facebookToken
import scala.language.postfixOps
import play.api.libs.functional.syntax._

case class Artist (artistId: Option[Long],
                   facebookId: Option[String],
                   name: String,
                   imagePath: Option[String] = None,
                   description: Option[String] = None,
                   facebookUrl: String,
                   websites: Set[String] = Set.empty,
                   genres: Seq[Genre] = Seq.empty,
                   tracks: Seq[Track] = Seq.empty,
                   likes: Option[Int] = None,
                   country: Option[String] = None)

object Artist {
  private val ArtistParser: RowParser[Artist] = {
    get[Long]("artistId") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("imagePath") ~
      get[Option[String]]("description") ~
      get[String]("facebookUrl") ~
      get[Option[String]]("websites") ~
      get[Option[Int]]("likes") ~
      get[Option[String]]("country") map {
      case artistId ~ facebookId ~ name ~ imagePath ~ description ~ facebookUrl ~ websites ~ likes ~ country =>
        Artist(Option(artistId), facebookId, name, imagePath, description, facebookUrl,
          Utilities.optionStringToSet(websites), Seq.empty, Seq.empty, likes, country)
    }
  }

  def formApply(facebookId: Option[String], name: String, imagePath: Option[String], description: Option[String],
                facebookUrl: String, websites: Seq[String], genres: Seq[Genre], tracks: Seq[Track], likes: Option[Int],
                country: Option[String]): Artist =
    Artist(None, facebookId, name, imagePath, description, facebookUrl, websites.toSet, genres, tracks, likes, country)
  def formUnapply(artist: Artist) =
    Option((artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
      artist.websites.toSeq, artist.genres, artist.tracks, artist.likes, artist.country))

  case class PatternAndArtist (searchPattern: String, artist: Artist)
  def formWithPatternApply(searchPattern: String, artist: Artist) =
    new PatternAndArtist(searchPattern, artist)
  def formWithPatternUnapply(searchPatternAndArtist: PatternAndArtist) =
    Option((searchPatternAndArtist.searchPattern, searchPatternAndArtist.artist))
  
  def getArtistProperties(artist: Artist): Artist = artist.copy(
      tracks = Track.findAllByArtist(artist.facebookUrl, 0, 0),
      genres = Genre.findAllByArtist(artist.artistId.getOrElse(-1L).toInt)
  )

  def findAll: Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists")
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.findAll: " + e.getMessage)
  }

  def findSinceOffset(numberOfArtistsToReturn: Int, offset: Int): Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM artists
           |  LIMIT $numberOfArtistsToReturn
           |  OFFSET $offset""".stripMargin)
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.find20Since: " + e.getMessage)
  }

  def findAllByEvent(event: Event): List[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM eventsArtists eA
          | INNER JOIN artists a ON a.artistId = eA.artistId
          | WHERE eA.eventId = {eventId}""".stripMargin)
        .on('eventId -> event.eventId)
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
  }

  def findByGenre(genre: String, numberToReturn: Int, offset: Int): Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT a.*
          |FROM artistsGenres aG
          |  INNER JOIN artists a ON a.artistId = aG.artistId
          |  INNER JOIN genres g ON g.genreId = aG.genreId
          |WHERE g.name = {genre}
          |LIMIT $numberToReturn OFFSET $offset""".stripMargin)
        .on('genre -> genre)
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.findAll: " + e.getMessage)
  }

  def find(artistId: Long): Option[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .as(ArtistParser.singleOpt)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.find: " + e.getMessage)
  }

  def findByFacebookUrl(facebookUrl: String): Option[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists WHERE facebookUrl = {facebookUrl}")
        .on('facebookUrl -> facebookUrl)
        .as(ArtistParser.singleOpt)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.findByFacebookUrl: " + e.getMessage)
  }

  def findAllContaining(searchPattern: String): Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM artists WHERE LOWER(name)
          |LIKE '%'||{searchPatternLowCase}||'%' LIMIT 10""".stripMargin)
        .on('searchPatternLowCase -> searchPattern.toLowerCase)
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.findAllContaining: " + e.getMessage)
  }

  def findIdByName(name: String): Long = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT artistId FROM artists WHERE name = {name}")
        .on('name -> name)
        .as(scalar[Long].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.findIdByName: " + e.getMessage)
  }

  def findIdByFacebookId(facebookId: String): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT artistId FROM artists WHERE facebookId = {facebookId}""")
        .on('facebookId -> facebookId)
        .as(scalar[Long].singleOpt)
    }
  }

  def findIdByFacebookUrl(facebookUrl: String)(implicit connection: Connection): Option[Long] = try {
    SQL("SELECT artistId FROM artists WHERE facebookUrl = {facebookUrl}")
      .on('facebookUrl -> facebookUrl)
      .as(scalar[Option[Long]].single)
  } catch {
    case e: Exception => throw new DAOException("Artist.findIdByFacebookUrl: " + e.getMessage)
  }

  def save(artist: Artist): Option[Long] = try {
    val websites: Option[String] = Utilities.setToOptionString(artist.websites)
    val description = Utilities.formatDescription(artist.description)
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertArtist({facebookId}, {name}, {imagePath}, {description}, {facebookUrl}, {websites})""")
        .on(
          'facebookId -> artist.facebookId,
          'name -> artist.name,
          'imagePath -> artist.imagePath,
          'facebookUrl -> artist.facebookUrl,
          'description -> description,
          'websites -> websites)
        .as(scalar[Long].singleOpt) match {
          case Some(artistId: Long) =>
            artist.genres.foreach { Genre.saveWithArtistRelation(_, artistId) }
            artist.tracks.foreach { Track.save }
            Option(artistId)
          case None =>
            None
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.save:\n" + e.getMessage)
  }

  def update(artist: Artist): Int = try {
    val websites: Option[String] = Utilities.setToOptionString(artist.websites)
    DB.withConnection { implicit connection =>
      SQL(
        """UPDATE artists
          | SET facebookId = {facebookId}, name = {name}, imagePath = {imagePath}, facebookUrl = {facebookUrl},
          |   description = {description}, websites = {websites}
          | WHERE artistId = {artistId}""".stripMargin)
        .on(
          'artistId -> artist.artistId,
          'facebookId -> artist.facebookId,
          'name -> artist.name,
          'imagePath -> artist.imagePath,
          'facebookUrl -> artist.facebookUrl,
          'description -> artist.description,
          'websites -> websites)
        .executeUpdate()
      }
  } catch {
    case e: Exception => throw new DAOException("Artist.update: " + e.getMessage)
  }

  def saveArtistsAndTheirTracks(artists: Seq[Artist]): Unit = Future {
    artists.map { artist =>
      val artistWithId = artist.copy(artistId = Artist.save(artist))
      getArtistTracks(PatternAndArtist(artistWithId.name, artistWithId)) |>> Iteratee.foreach( a => a.map { Track.save })
    }
  }

  def getArtistTracks(patternAndArtist: PatternAndArtist): Enumerator[Set[Track]] = {
    val soundCloudTracksEnumerator = Enumerator.flatten(
      getSoundCloudTracksForArtist(patternAndArtist.artist).map { soundCloudTracks =>
        Artist.addSoundCloudWebsiteIfMissing(soundCloudTracks.headOption, patternAndArtist.artist)
        Enumerator(soundCloudTracks.toSet)
      })

    val youtubeTracksEnumerator =
      getYoutubeTracksForArtist(patternAndArtist.artist, patternAndArtist.searchPattern)

    Enumerator.interleave(soundCloudTracksEnumerator, youtubeTracksEnumerator).andThen(Enumerator.eof)
  }

  def addWebsite(artistId: Option[Long], normalizedUrl: String): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """UPDATE artists
          |  SET websites = case
          |    WHEN websites IS NULL THEN {normalizedUrl}
          |    ELSE websites || ',' || {normalizedUrl}
          |  END
          |WHERE artistId = {artistId}""".stripMargin)
        .on('artistId -> artistId,
            'normalizedUrl -> normalizedUrl)
        .executeUpdate()
    }
  }

  def addSoundCloudWebsiteIfMissing(soundCloudTrack: Option[Track], artist: Artist): Unit = soundCloudTrack match {
    case None =>
    case Some(soundCloudTrack: Track) =>
      soundCloudTrack.redirectUrl match {
        case None =>
        case Some(redirectUrl) =>
          val refactoredRedirectUrl = removeUselessInSoundCloudWebsite(Utilities.normalizeUrl(redirectUrl))
          if (!artist.websites.contains(refactoredRedirectUrl))
            Artist.addWebsite(artist.artistId, refactoredRedirectUrl)
      }
  }

  def saveWithEventRelation(artist: Artist, eventId: Long): Boolean = save(artist) match {
    case None => false
    case Some(artistId: Long) => saveEventRelation(eventId, artistId)
  }

  def saveEventRelation(eventId: Long, artistId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertEventArtistRelation({eventId}, {artistId})""")
        .on(
          'eventId -> eventId,
          'artistId -> artistId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.saveEventArtistRelation: " + e.getMessage)
  }

  def deleteEventRelation(eventId: Long, artistId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(s"""DELETE FROM eventsArtists WHERE eventId = $eventId AND artistId = $artistId""")
        .executeUpdate()
    }
  }
  
  def delete(artistId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.delete : " + e.getMessage)
  }

  def followByArtistId(userUUID : UUID, artistId : Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO artistsFollowed(userId, artistId) VALUES({userId}, {artistId})""")
        .on(
          'userId -> userUUID,
          'artistId -> artistId)
        .executeInsert()
    }
  }

  def unfollowByArtistId(userUUID: UUID, artistId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM artistsFollowed
          | WHERE userId = {userId} AND artistId = {artistId}""".stripMargin)
        .on('userId -> userUUID,
            'artistId -> artistId)
        .executeUpdate()
    }
  }

  def followByFacebookId(userUUID : UUID, facebookId: String): Try[Option[Long]] =
    findIdByFacebookId(facebookId) match {
        case Success(None) => Failure(ThereIsNoArtistForThisFacebookIdException("Artist.followByFacebookId"))
        case Success(Some(artistId)) => followByArtistId(userUUID, artistId)
        case failure => failure
    }

  def getFollowedArtists(userUUID : UUID): Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("""select a.* from artists a
            |  INNER JOIN artistsFollowed af ON a.artistId = af.artistId
            |WHERE af.userId = {userId}""".stripMargin)
        .on('userId -> userUUID)
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.getFollowedArtists: " + e.getMessage)
  }

  def isFollowed(userUUID : UUID, artistId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM artistsFollowed
          |  WHERE userId = {userId} AND artistId = {artistId})""".stripMargin)
        .on("userId" -> userUUID,
          "artistId" -> artistId)
        .as(scalar[Boolean].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.isArtistFollowed: " + e.getMessage)
  }

  def normalizeArtistName(artistName: String): String = {
    normalizeString(artistName)
      .toLowerCase
      .replaceAll("officiel", "")
      .replaceAll("fanpage", "")
      .replaceAll("official", "")
      .replaceAll("fb", "")
      .replaceAll("facebook", "")
      .replaceAll("page", "")
      .trim
      .replaceAll("""\s+""", " ")
  }

  def splitArtistNamesInTitle(title: String): List[String] =
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "")

  val facebookArtistFields = "name,cover{source,offset_x,offset_y},id,category,link,website,description,genre,location,likes"


  def getEventuallyFacebookArtists(pattern: String): Future[Seq[Artist]] = {
    WS.url("https://graph.facebook.com/v2.2/search")
      .withQueryString(
        "q" -> pattern,
        "type" -> "page",
        "limit" -> "400",
        "fields" -> facebookArtistFields,
        "access_token" -> facebookToken)
      .get()
      .map { readFacebookArtists }
  }

  def getEventuallyArtistsInEventTitle(artistsNameInTitle: Seq[String], webSites: Set[String]): Future[Seq[Artist]] = {
    Future.sequence(
      artistsNameInTitle.map {
        getEventuallyFacebookArtists(_).map { artists => artists }
      }
    ).map { _.flatten collect { case artist: Artist if (artist.websites intersect webSites).nonEmpty => artist } }
  }

  def getFacebookArtistsByWebsites(websites: Set[String]): Future[Set[Option[Artist]]] = {
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
      .withQueryString("client_id" -> soundCloudClientId)
      .get()
      .map { readMaybeFacebookUrl }
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesWSResponse: WSResponse): Option[String] = {
    val facebookUrlReads = (
      (__ \ "url").read[String] and
        (__ \ "service").read[String]
      )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
      urlService.collect { case (url: String, "facebook") => normalizeUrl(url) }
    }

    soundCloudWebProfilesWSResponse.json.asOpt[Seq[String]](collectOnlyFacebookUrls) match {
      case Some(facebookUrls: Seq[String]) if facebookUrls.nonEmpty => Option(facebookUrls.head)
      case _ => None
    }
  }

  def getFacebookArtistByFacebookUrl(url: String): Future[Option[Artist]] = {
    WS.url("https://graph.facebook.com/v2.2/" + normalizeFacebookUrl(url))
      .withQueryString(
        "fields" -> facebookArtistFields,
        "access_token" -> facebookToken)
      .get()
      .map { readFacebookArtist }
  }

  def normalizeFacebookUrl(facebookUrl: String): String = {
    val firstNormalization = facebookUrl.drop(facebookUrl.lastIndexOf("/") + 1) match {
      case urlWithProfile: String if urlWithProfile contains "profile.php?id=" =>
        urlWithProfile.substring(urlWithProfile.lastIndexOf("=") + 1)
      case alreadyNormalizedUrl: String =>
        alreadyNormalizedUrl
    }
    firstNormalization match {
      case urlWithArguments if urlWithArguments contains "?" =>
        urlWithArguments.slice(0, urlWithArguments.lastIndexOf("?"))
      case urlWithoutArguments =>
        urlWithoutArguments
    }
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

  def readFacebookArtists(facebookWSResponse: WSResponse): Seq[Artist] = {
    val collectOnlyArtistsWithCover: Reads[Seq[Artist]] = Reads.seq(readArtist).map { artists =>
      artists.collect {
        case (name, facebookId, category, Some(cover: String), maybeOffsetX, maybeOffsetY, websites, link,
        maybeDescription, maybeGenre, maybeLikes, maybeCountry)
          if category.equalsIgnoreCase("Musician/band") | category.equalsIgnoreCase("Artist") =>
          makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), websites, link,
            maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten)
      }
    }
    (facebookWSResponse.json \ "data")
      .asOpt[Seq[Artist]](collectOnlyArtistsWithCover)
      .getOrElse(Seq.empty)
  }

  def readFacebookArtist(facebookWSResponse: WSResponse): Option[Artist] = {
    facebookWSResponse.json
      .asOpt[(String, String, String, Option[String], Option[Int], Option[Int], Option[String],
      String, Option[String], Option[String], Option[Int], Option[Option[String]])](readArtist)
    match {
      case Some((name, facebookId, "Musician/band", Some(cover: String), maybeOffsetX, maybeOffsetY, maybeWebsites,
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
                 maybeCountry: Option[String]): Artist = {
    val facebookUrl = normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
    val websitesSet = getNormalizedWebsitesInText(maybeWebsites)
      .filterNot(_.contains("facebook.com"))
      .filterNot(_ == "")
    val description = Utilities.formatDescription(maybeDescription)
    val genres = genresStringToGenresSet(maybeGenre)
    Artist(None, Option(facebookId), name, Option(cover), description, facebookUrl, websitesSet, genres.toSeq,
      Seq.empty, maybeLikes, maybeCountry)
  }

  def aggregateImageAndOffset(imgUrl: String, offsetX: Option[Int], offsetY: Option[Int]): String =
    imgUrl + """\""" + offsetX.getOrElse(0).toString + """\""" + offsetY.getOrElse(0).toString
}
