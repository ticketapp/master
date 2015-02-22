package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import controllers.WebServiceException
import services.Utilities
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import play.api.libs.ws.Response
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import scala.util.{Failure, Success}

case class Artist (artistId: Long,
                   creationDateTime: Date,
                   facebookId: Option[String],
                   name: String,
                   description: Option[String],
                   images: List[Image],
                   genres: List[Genre],
                   tracks: List[Track])


object Artist {
  val token = play.Play.application.configuration.getString("facebook.token")

  private val ArtistParser: RowParser[Artist] = {
    get[Long]("artistId") ~
      get[Date]("creationDateTime") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("description") map {
      case artistId ~ creationDateTime ~ facebookId ~ name ~ description =>
        Artist(artistId, creationDateTime, facebookId, name, description, List(), List(), List())
    }
  }

  def formApply(facebookId: Option[String], name: String): Artist =
    new Artist(-1L, new Date, facebookId, name, None, List(), List(), List())
  def formUnapply(artist: Artist): Option[(Option[String], String)] = Some((artist.facebookId, artist.name))

  def findAll(): List[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("select * from artists").as(ArtistParser.*)
    }
  }

  def findAllByEvent(event: Event): List[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsArtists eA
             INNER JOIN artists a ON a.artistId = eA.artistId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(ArtistParser.*)
    }
  }

  def find(artistId: Long): Option[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .as(ArtistParser.singleOpt)
    }
  }

  def findAllContaining(pattern: String): Seq[Artist] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM artists WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(ArtistParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Artist.findAllContaining: " + e.getMessage)
    }
  }

  def save(artist: Artist): Option[Long] = {
    Utilities.testIfExist("artists", "name", artist.name) match {
      case true => Some(-1)
      case false => try {
        DB.withConnection { implicit connection =>
          SQL("insert into artists(name, facebookId) values ({name}, {facebookId})").on(
            'name -> artist.name,
            'facebookId -> artist.facebookId
          ).executeInsert() match {
            case None => None
            case Some(artistId: Long) =>
              artist.images.foreach(image =>
                Image.save(image.copy(artistId = Some(artistId)))
              )
              artist.genres.foreach(genre =>
                Genre.saveGenreAndArtistRelation(genre, artistId)
              )
              artist.tracks.foreach(track =>
                Track.saveTrackAndArtistRelation(track, artistId)
              )
              Some(artistId)
          }
        }
      } catch {
        case e: Exception => throw new DAOException("Cannot create artist: " + e.getMessage)
      }
    }
  }

  def returnArtistId(name: String): Long = {
    DB.withConnection { implicit connection =>
      SQL("SELECT artistId from artists WHERE name = {name}")
        .on('name -> name)
        .as(scalar[Long].single)
    }
  }

  def saveWithEventRelation(artist: Artist, eventId: Long): Option[Long] = {
    save(artist) match {
      case Some(-1) => saveEventArtistRelation(eventId, returnArtistId(artist.name))
      case Some(i) => saveEventArtistRelation(eventId, i)
      case None => None
    }
  }

  def saveEventArtistRelation(eventId: Long, artistId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO eventsArtists (eventId, artistId)
          VALUES ({eventId}, {artistId})""").on(
            'eventId -> eventId,
            'artistId -> artistId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save in eventsArtists : " + e.getMessage)
    }
  }


  def deleteArtist(artistId: Long): Long = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM artists WHERE artistId={artistId}").on(
        'artistId -> artistId
      ).executeUpdate()
    }
  }

  def followArtist(userId : Long, artistId : Long): Option[Long] = {
    try {
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
}