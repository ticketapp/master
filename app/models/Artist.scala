package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import controllers.{ThereIsNoArtistForThisFacebookIdException, DAOException, WebServiceException}
import securesocial.core.IdentityId
import services.SearchSoundCloudTracks._
import services.SearchYoutubeTracks._
import services.Utilities
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import play.api.libs.ws.Response
import services.Utilities._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import scala.util.{Try, Failure, Success}
import services.Utilities.facebookToken

case class Artist (artistId: Option[Long],
                   facebookId: Option[String],
                   name: String,
                   imagePath: Option[String] = None,
                   description: Option[String] = None,
                   facebookUrl: String,
                   websites: Set[String] = Set.empty,
                   genres: Seq[Genre] = Seq.empty,
                   tracks: Seq[Track] = Seq.empty)

object Artist {
  private val ArtistParser: RowParser[Artist] = {
    get[Long]("artistId") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("imagePath") ~
      get[Option[String]]("description") ~
      get[String]("facebookUrl") ~
      get[Option[String]]("websites") map {
      case artistId ~ facebookId ~ name ~ imagePath ~ description ~ facebookUrl ~ websites =>
        Artist(Option(artistId), facebookId, name, imagePath, description, facebookUrl,
          websites.getOrElse("").split(",").toSet, Seq.empty, Seq.empty)
    }
  }

  def formApply(facebookId: Option[String], name: String, imagePath: Option[String], description: Option[String],
                facebookUrl: String, websites: Seq[String], genres: Seq[Genre], tracks: Seq[Track]): Artist =
    new Artist(None, facebookId, name, imagePath, description, facebookUrl, websites.toSet, genres, tracks)
  def formUnapply(artist: Artist) =
    Option((artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
      artist.websites.toSeq, artist.genres, artist.tracks))

  case class PatternAndArtist (searchPattern: String, artist: Artist)
  def formWithPatternApply(searchPattern: String, artist: Artist) =
    new PatternAndArtist(searchPattern, artist)
  def formWithPatternUnapply(searchPatternAndArtist: PatternAndArtist) =
    Option((searchPatternAndArtist.searchPattern, searchPatternAndArtist.artist))
  
  def getArtistProperties(artist: Artist): Artist = artist.copy(
      tracks = Track.findAllByArtist(artist.facebookUrl),
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
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
  }

  def find(artistId: Long): Option[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .as(ArtistParser.singleOpt)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
  }

  def findByFacebookUrl(facebookUrl: String): Option[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists WHERE facebookUrl = {facebookUrl}")
        .on('facebookUrl -> facebookUrl)
        .as(ArtistParser.singleOpt)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
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

  def save(artist: Artist): Option[Long] = try {
    DB.withConnection { implicit connection =>
      val websites: Option[String] = if (artist.websites.isEmpty) None else Option(artist.websites.mkString(","))
      SQL(
        """SELECT insertArtist({facebookId}, {name}, {imagePath}, {description}, {facebookUrl}, {websites})""")
        .on(
          'facebookId -> artist.facebookId,
          'name -> artist.name,
          'imagePath -> artist.imagePath,
          'facebookUrl -> artist.facebookUrl,
          'description -> artist.description,
          'websites -> websites)
        .as(scalar[Long].singleOpt) match {
          case Some(artistId: Long) =>
            artist.genres.foreach { Genre.saveWithArtistRelation(_, artistId.toInt) }
            artist.tracks.foreach { Track.save }
            Option(artistId)
          case None => None
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.save: " + e.getMessage)
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

  def saveWithEventRelation(artist: Artist, eventId: Long): Boolean = save(artist) match {
    case None => false
    case Some(artistId: Long) => saveEventArtistRelation(eventId, artistId)
  }

  def returnArtistId(name: String): Long = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT artistId FROM artists WHERE name = {name}")
        .on('name -> name)
        .as(scalar[Long].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot returnArtistId: " + e.getMessage)
  }

  def returnArtistIdByFacebookUrl(facebookUrl: String)(implicit connection: Connection): Option[Long] = try {
    SQL("SELECT artistId FROM artists WHERE facebookUrl = {facebookUrl}")
      .on('facebookUrl -> facebookUrl)
      .as(scalar[Option[Long]].single)
  } catch {
    case e: Exception => throw new DAOException("Cannot returnArtistIdByFacebookUrl: " + e.getMessage)
  }

  def saveEventArtistRelation(eventId: Long, artistId: Long): Boolean = try {
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

  def delete(artistId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.delete : " + e.getMessage)
  }

  def followByArtistId(userId : String, artistId : Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO artistsFollowed(userId, artistId) VALUES({userId}, {artistId})""")
        .on(
          'userId -> userId,
          'artistId -> artistId)
        .executeInsert()
    }
  }

  def unfollowByArtistId(userId: String, artistId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM artistsFollowed
          | WHERE userId = {userId} AND artistId = {artistId}""".stripMargin)
        .on('userId -> userId,
            'artistId -> artistId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.unFollow: " + e.getMessage)
  }

  def followByFacebookId(userId : String, facebookId: String): Try[Option[Long]] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT artistId FROM artists WHERE facebookId = {facebookId}""")
        .on('facebookId -> facebookId)
        .as(scalar[Long].singleOpt) match {
        case None => throw ThereIsNoArtistForThisFacebookIdException("Artist.followArtistByFacebookId")
        case Some(artistId) => followByArtistId(userId, artistId)
      }
    }
  } catch {
    case thereIsNoArtistForThisFacebookIdException: ThereIsNoArtistForThisFacebookIdException =>
      throw ThereIsNoArtistForThisFacebookIdException("Artist.followByFacebookId")
    case e: Exception =>
      throw DAOException("Artist.followByFacebookId: " + e.getMessage)
  }

  def getFollowedArtists(userId: IdentityId): Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("""select a.* from artists a
            |  INNER JOIN artistsFollowed af ON a.artistId = af.artistId
            |WHERE af.userId = {userId}""".stripMargin)
        .on('userId -> userId.userId)
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.getFollowedArtists: " + e.getMessage)
  }

  def isFollowed(userId: IdentityId, artistId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM artistsFollowed
          |  WHERE userId = {userId} AND artistId = {artistId})""".stripMargin)
        .on("userId" -> userId.userId,
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
}
