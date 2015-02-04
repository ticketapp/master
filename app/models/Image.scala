package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.{Json, JsNull, Writes}
import play.api.Play.current

import scala.util.Try

case class Image (imageId: Long,
                  path: String,
                  eventId: Option[Long] = None,
                  userId: Option[Long] = None)


object Image {

  implicit val imageWrites = Json.writes[Image]

  def formApply(path: String,  alt: String) = new Image(-1L, path)
  def formUnapply(image: Image) = Some((image.path))

  private val ImageParser: RowParser[Image] = {
    get[Long]("imageId") ~
    get[String]("path") ~
    get[Option[Long]]("eventId") ~
    get[Option[Long]]("userId") map {
      case imageId ~ path ~ eventId ~ userId =>
        Image(imageId, path, eventId, userId)
    }
  }

  def findAll(): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select * from images").as(ImageParser *)
    }
  }

  def findAllByEvent(event: Event): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select * from images where eventId = {eventId}")
        .on('eventId -> event.eventId)
        .as(ImageParser *)
    }
  }

  def findAllByPlace(placeId: Long): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM Images
             WHERE placeId = {placeId}""")
        .on('placeId -> placeId)
        .as(ImageParser *)
    }
  }

  def find(imageId: Long): Option[Image] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from images WHERE placeId = {imageId}")
        .on('imageId -> imageId)
        .as(ImageParser.singleOpt)
    }
  }

  def save(image: Image) = {
    DB.withConnection { implicit connection =>
      SQL(""" INSERT INTO images(path, eventId, userId) VALUES({path}, {eventId}, {userId}) """)
        .on(
          'path -> image.path,
          'eventId -> image.eventId,
          'userId -> image.userId
        ).executeInsert()
    }
  }
}
