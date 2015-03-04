package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import services.Utilities

case class Image (imageId: Long,
                  path: String,
                  eventId: Option[Long] = None,
                  userId: Option[Long] = None,
                  artistId: Option[Long] = None,
                  placeId: Option[Long] = None,
                  organizerId: Option[Long] = None)

object Image {
  def formApply(path: String) = new Image(-1L, path)

  def formUnapply(image: Image) = Some(image.path)

  private val ImageParser: RowParser[Image] = {
    get[Long]("imageId") ~
      get[String]("path") ~
      get[Option[Long]]("eventId") ~
      get[Option[Long]]("userId") ~
      get[Option[Long]]("placeId") ~
      get[Option[Long]]("organizerId") map {
      case imageId ~ path ~ eventId ~ userId ~ placeId ~ organizerId=>
        Image(imageId, path, eventId, userId, placeId, organizerId)
    }
  }

  def findAll(): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select * from images").as(ImageParser.*)
    }
  }

  def findAllByEvent(event: Event): List[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select * from images where eventId = {eventId}")
        .on('eventId -> event.eventId)
        .as(ImageParser.*)
    }
  }

  def findAllByPlace(placeId: Long): List[Image] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM Images
             WHERE placeId = {placeId}""")
        .on('placeId -> placeId)
        .as(ImageParser.*)
    }
  }

  def findAllByArtist(artistId: Long): Set[Image] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM Images
             WHERE artistId = {artistId}""")
        .on('artistId -> artistId)
        .as(ImageParser.*)
        .toSet
    }
  }
  
  def findAllByOrganizer(organizerId: Long): List[Image] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM Images
             WHERE organizerId = {organizerId}""")
        .on('organizerId -> organizerId)
        .as(ImageParser.*)
    }
  }

  def find(imageId: Long): Option[Image] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from images WHERE placeId = {imageId}")
        .on('imageId -> imageId)
        .as(ImageParser.singleOpt)
    }
  }

  def save(image: Image): Option[Long] = {
    println(image)
    Utilities.testIfExist("images", "path", image.path) match {
      case true =>
        println("Image path already in database")
        None
      case false => try {
        DB.withConnection { implicit connection =>
          SQL("""INSERT INTO images(path, eventId, userId, placeId, organizerId, artistId)
              VALUES({path}, {eventId}, {userId}, {placeId}, {organizerId}, {artistId})""")
            .on(
              'path -> image.path,
              'eventId -> image.eventId,
              'userId -> image.userId,
              'placeId -> image.placeId,
              'organizerId -> image.organizerId,
              'artistId -> image.artistId
            ).executeInsert()
        }
      } catch {
        case e: Exception => throw new DAOException("Cannot save image: " + e.getMessage)
      }
    }
  }

  def saveEventImageRelation(eventId: Long, imageId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO eventsImages (eventId, imageId)
            VALUES ({eventId}, {imageId})""").on(
            'eventId -> eventId,
            'imageId -> imageId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("saveEventImageRelation: " + e.getMessage)
    }
  }
}


