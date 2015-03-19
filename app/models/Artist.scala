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
                   description: Option[String] = None,
                   facebookUrl: String,
                   websites: Set[String] = Set.empty,
                   images: Set[Image] = Set.empty,
                   genres: Seq[Genre] = Seq.empty,
                   tracks: Seq[Track] = Seq.empty)

object Artist {
  val token = play.Play.application.configuration.getString("facebook.token")

  private val ArtistParser: RowParser[Artist] = {
    get[Long]("artistId") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("description") ~
      get[String]("facebookUrl") ~
      get[Option[String]]("websites") map {
      case artistId ~ facebookId ~ name ~ description ~ facebookUrl ~ websites =>
        Artist(Option(artistId), facebookId, name, description, facebookUrl,
          websites.getOrElse("").split(",").toSet, Set(), Seq.empty, Seq.empty)
    }
  }

  def formApply(facebookId: Option[String], name: String, description: Option[String], facebookUrl: String,
                websites: Seq[String], images: Seq[Image], genres: Seq[Genre], tracks: Seq[Track]): Artist =
    new Artist(None, facebookId, name, description, facebookUrl, websites.toSet, images.toSet, genres, tracks)
  def formUnapply(artist: Artist) =
    Option((artist.facebookId, artist.name, artist.description, artist.facebookUrl, artist.websites.toSeq,
      artist.images.toSeq, artist.genres.toSeq, artist.tracks.toSeq))

  case class PatternAndArtist (searchPattern: String, artist: Artist)
  def formWithPatternApply(searchPattern: String, artist: Artist) = new PatternAndArtist(searchPattern, artist)
  def formWithPatternUnapply(searchPatternAndArtist: PatternAndArtist) =
    Option((searchPatternAndArtist.searchPattern, searchPatternAndArtist.artist))
  
  def artistWithProperties(artist: Artist): Artist = artist.copy(
      images = Image.findAllByArtist(artist.artistId.getOrElse(-1)),
      tracks = Track.findAllByArtist(artist.facebookUrl),
      genres = Genre.findAllByArtist(artist.artistId.getOrElse(-1L).toInt)
  )

  def findAll: Seq[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists")
        .as(ArtistParser.*)
        .map(artistWithProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
  }

  def findAllByEvent(event: Event): List[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsArtists eA
             INNER JOIN artists a ON a.artistId = eA.artistId
             WHERE eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(ArtistParser.*)
        .map(artistWithProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
  }

  def find(artistId: Long): Option[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .as(ArtistParser.singleOpt)
        .map(artistWithProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with method Artist.findAll: " + e.getMessage)
  }

  def findByFacebookUrl(facebookUrl: String): Option[Artist] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM artists WHERE facebookUrl = {facebookUrl}")
        .on('facebookUrl -> facebookUrl)
        .as(ArtistParser.singleOpt)
        .map(artistWithProperties)
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
        .map(artistWithProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.findAllContaining: " + e.getMessage)
  }

  def save(artist: Artist): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertArtist({facebookId}, {name}, {description}, {facebookUrl}, {websites})""")
        .on(
          'facebookId -> artist.facebookId,
          'name -> artist.name,
          'facebookUrl -> artist.facebookUrl,
          'description -> artist.description,
          'websites -> artist.websites.mkString(","))
        .as(scalar[Option[Long]].single) match {
          case Some(artistId: Long) =>
            artist.images.foreach { image => Image.save(image.copy(artistId = Some(artistId))) }
            artist.genres.foreach { Genre.saveWithArtistRelation(_, artistId.toInt) }
            artist.tracks.foreach { Track.save }
            Option(artistId)
          case None => None
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Artist.save: " + e.getMessage)
  }

  def saveWithEventRelation(artist: Artist, eventId: Long): Option[Long] = save(artist) match {
    case None => None
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


  def saveEventArtistRelation(eventId: Long, artistId: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO eventsArtists (eventId, artistId)
        VALUES ({eventId}, {artistId})""")
        .on(
          'eventId -> eventId,
          'artistId -> artistId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot save in eventsArtists : " + e.getMessage)
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
      SQL("insert into artistsFollowed(userId, artistId) values ({userId}, {artistId})").on(
        'userId -> userId,
        'artistId -> artistId
      ).executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot follow artist: " + e.getMessage)
  }
}
