package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.{Json, JsNull, Writes}
import play.api.Play.current

case class Image (imageId: Long,
                  path: String,
                  alt: String,
                  eventId: Option[Long] = None,
                  userId: Option[Long] = None)


object Image {

  implicit val imageWrites = Json.writes[Image]

  def formApply(path: String,  alt: String) = new Image(-1L, path, alt)
  def formUnapply(image: Image) = Some((image.path, image.alt))

  private val ImageParser: RowParser[Image] = {
    get[Long]("imageId") ~
    get[String]("path") ~
    get[String]("alt") ~
    get[Option[Long]]("eventId") ~
    get[Option[Long]]("userId") map {
      case placeId ~ path ~ alt ~ eventId ~ userId =>
        Image(placeId, path, alt, eventId, userId)
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

  def find(imageId: Long): Option[Image] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from images WHERE placeId = {imageId}")
        .on('imageId -> imageId)
        .as(ImageParser.singleOpt)
    }
  }
/*
  def save(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
            INSERT INTO images(name)
            VALUES({name})
          """).on(
          'name -> name
        ).executeUpdate
    }
  }
  */

}
