package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import controllers.WebServiceException
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
import scala.util.{Failure, Success}

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
  val token = play.Play.application.configuration.getString("facebook.token")

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
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
  }

  def findSinceOffset(numberOfArtistsToReturn: Int, offset: Int): Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT *
           |FROM artists
           |LIMIT $numberOfArtistsToReturn OFFSET $offset""".stripMargin)
        .as(ArtistParser.*)
        .map(getArtistProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.find20Since: " + e.getMessage)
  }

  def findAllByEvent(event: Event): List[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsArtists eA
             INNER JOIN artists a ON a.artistId = eA.artistId
             WHERE eA.eventId = {eventId}""")
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
      SQL(
        """SELECT insertArtist({facebookId}, {name}, {imagePath}, {description}, {facebookUrl}, {websites})""")
        .on(
          'facebookId -> artist.facebookId,
          'name -> artist.name,
          'imagePath -> artist.imagePath,
          'facebookUrl -> artist.facebookUrl,
          'description -> artist.description,
          'websites -> artist.websites.mkString(","))
        .as(scalar[Option[Long]].single) match {
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

  def deleteArtist(artistId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM artists WHERE artistId={artistId}").on(
        'artistId -> artistId
      ).executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot save in deleteArtist : " + e.getMessage)
  }

  def followArtist(userId : Long, artistId : Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO artistsFollowed(userId, artistId) VALUES ({userId}, {artistId})").on(
        'userId -> userId,
        'artistId -> artistId
      ).executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot follow artist: " + e.getMessage)
  }
}
