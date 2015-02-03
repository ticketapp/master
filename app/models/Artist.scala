package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date


/**
 * Created by simon on 03/10/14.
 */
case class Artist (artistId: Long,
                   creationDateTime: Date,
                   facebookId: Option[String],
                   name: String,
                   description: Option[String])


object Artist {
  implicit val artistWrites = Json.writes[Artist]

  private val ArtistParser: RowParser[Artist] = {
    get[Long]("artistId") ~
      get[Date]("creationDateTime") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("description") map {
      case artistId ~ creationDateTime ~ facebookId ~ name ~ description =>
        Artist(artistId, creationDateTime, facebookId, name, description)
    }
  }

  def findAll(): Seq[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("select * from artists").as(ArtistParser *)
    }
  }

  def findAllByEvent(event: Event): Seq[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsArtists eA
             INNER JOIN artists a ON a.artistId = eA.artistId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(ArtistParser *)
    }
  }

  def find(artistId: Long): Option[Artist] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from artists WHERE artistId = {artistId}")
        .on('artistId -> artistId)
        .as(ArtistParser.singleOpt)
    }
  }


  def findAllStartingWith(pattern: String): Seq[Artist] = {
    /*

    Security with the string? Need to escape it?


     */
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM artists WHERE LOWER(name) LIKE {patternLowCase} || '%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(ArtistParser *)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Artist.findAllStartingWith: " + e.getMessage)
    }
  }

  def formApply(facebookId: Option[String], name: String): Artist = new Artist(-1L, new Date, facebookId, name, None)
  def formUnapply(artist: Artist): Option[(Option[String], String)] = Some((artist.facebookId, artist.name))

  def saveArtist(artist: Artist): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into artists(name, facebookId) values ({name}, {facebookId})").on(
          'name -> artist.name,
          'facebookId -> artist.facebookId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create artist: " + e.getMessage)
    }
  }


  def deleteArtist(artistId: Long): Long = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM artists WHERE artistId={artistId}").on(
        'artistId -> artistId
      ).executeUpdate()
    }
  }

  def followArtist(userId : Long, artistId : Long): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into artistsFollowed(userId, artistId) values ({userId}, {artistId})").on(
          'userId -> userId,
          'artistId -> artistId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow artist: " + e.getMessage)
    }
  }
}