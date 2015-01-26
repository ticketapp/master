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
                   facebookId: Long,
                   name: String)


object Artist {
  implicit val artistWrites = Json.writes[Artist]

  private val ArtistParser: RowParser[Artist] = {
    get[Long]("artistId") ~
      get[Date]("creationDateTime") ~
      get[Long]("facebookId") ~
      get[String]("name") map {
      case artistId ~ creationDateTime ~ facebookId ~ name =>
        Artist(artistId, creationDateTime, facebookId, name)
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
        .on('eventId -> event.id)
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

  def formApply(facebookId: Long, name: String): Artist = new Artist(-1L, new Date, facebookId, name)
  def formUnapply(artist: Artist): Option[(Long, String)] = Some((artist.facebookId, artist.name))

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