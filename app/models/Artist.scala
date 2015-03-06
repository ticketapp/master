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
                   facebookId: Option[String],
                   name: String,
                   description: Option[String] = None,
                   websites: Set[String] = Set.empty,
                   images: Set[Image] = Set.empty,
                   genres: Set[Genre] = Set.empty,
                   tracks: Set[Track] = Set.empty)

object Artist {
  val token = play.Play.application.configuration.getString("facebook.token")

  private val ArtistParser: RowParser[Artist] = {
    get[Long]("artistId") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("description") ~
      get[Option[String]]("websites") map {
      case artistId ~ facebookId ~ name ~ description ~ websites =>
        Artist(artistId, facebookId, name, description, websites.getOrElse("").split(",").toSet, Set(), Set(), Set())
    }
  }

  def formApply(facebookId: Option[String], name: String, websites: Seq[String], images: Seq[Image], genres: Seq[Genre],
                tracks: Seq[Track]): Artist =
    new Artist(-1L, facebookId, name, None, websites.toSet, images.toSet, genres.toSet, tracks.toSet)
  def formUnapply(artist: Artist): Option[(Option[String], String, Seq[String], Seq[Image], Seq[Genre], Seq[Track])] =
    Some((artist.facebookId, artist.name, artist.websites.toSeq, artist.images.toSeq, artist.genres.toSeq,
      artist.tracks.toSeq))

  def findAll(): List[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("select * from artists")
        .as(ArtistParser.*)
        .map(artist =>
          artist.copy(
            images = Image.findAllByArtist(artist.artistId),
            genres = Genre.findAllByArtist(artist.artistId),
            tracks = Track.findAllByArtist(artist.artistId)
          )
        )
    }
  }

  def findAllByEvent(event: Event): List[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsArtists eA
             INNER JOIN artists a ON a.artistId = eA.artistId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(ArtistParser.*)
        .map(artist =>
          artist.copy(
            images = Image.findAllByArtist(artist.artistId),
            genres = Genre.findAllByArtist(artist.artistId),
            tracks = Track.findAllByArtist(artist.artistId)
          )
        )
    }
  }

  def find(artistId: Long): Option[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .as(ArtistParser.singleOpt)
        .map(artist =>
          artist.copy(
            images = Image.findAllByArtist(artistId),
            genres = Genre.findAllByArtist(artistId),
            tracks = Track.findAllByArtist(artistId)
          )
        )
    }
  }

  def findAllContaining(pattern: String): Set[Artist] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM artists WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(ArtistParser.*)
          .map(artist =>
            artist.copy(
              images = Image.findAllByArtist(artist.artistId),
              genres = Genre.findAllByArtist(artist.artistId),
              tracks = Track.findAllByArtist(artist.artistId)
            )
          )
          .toSet
      }
    } catch {
      case e: Exception =>
        throw new DAOException("Problem with the method Artist.findAllContaining: " + e.getMessage)
    }
  }

  def save(artist: Artist): Option[Long] = {
    Utilities.testIfExist("artists", "name", artist.name) match {
      case true => Some(-1)
      case false => try {
        DB.withConnection { implicit connection =>
          SQL(
            """INSERT INTO artists(facebookId, name, description, websites)
              VALUES ({facebookId}, {name}, {description}, {websites})""")
            .on(
              'facebookId -> artist.facebookId,
              'name -> artist.name,
              'description -> artist.description,
              'websites -> artist.websites.mkString(","))
            .executeInsert() match {
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