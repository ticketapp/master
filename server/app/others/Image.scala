package others

import javax.inject.Inject

import database.MyPostgresDriver
import organizersDomain.OrganizerMethods
import placesDomain.PlaceMethods
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.language.postfixOps

case class Image (id: Long,
                  path: String,
                  eventId: Option[Long] = None,
                  userId: Option[Long] = None,
                  artistId: Option[Long] = None,
                  placeId: Option[Long] = None,
                  organizerId: Option[Long] = None)

class ImageMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val placeMethods: PlaceMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver] {


//  class Images(tag: Tag) extends Table[Image](tag, "images") {
//    def id = column[Long]("imageid", O.PrimaryKey, O.AutoInc)
//    def path = column[String]("path")
////    def userId = column[Option[String]]("path")
//
//    def * = (id, name, icon) <> ((Image.apply _).tupled, Image.unapply)
//  }
//
//  lazy val images = TableQuery[Images]
//
  def formApply(path: String) = new Image(-1L, path)

  def formUnapply(image: Image) = Some(image.path)


//
//  def findAll: Seq[Image] = try {
//    DB.withConnection { implicit connection =>
//      SQL("SELECT * FROM images").as(ImageParser.*)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Image.findAll: " + e.getMessage)
//  }
//
//  def findAllByEvent(event: Event): List[Image] = try {
//    DB.withConnection { implicit connection =>
//      SQL("SELECT * FROM images WHERE eventId = {eventId}")
//        .on('eventId -> event.eventId)
//        .as(ImageParser.*)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Image.findAllByEvent: " + e.getMessage)
//  }
//
//  def findAllByArtist(artistId: Long): Set[Image] = try {
//    DB.withConnection { implicit connection =>
//      SQL("""SELECT *
//             FROM Images
//             WHERE artistId = {artistId}""")
//        .on('artistId -> artistId)
//        .as(ImageParser.*)
//        .toSet
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Image.findAllByArtist: " + e.getMessage)
//  }
//
//  def findAllByOrganizer(organizerId: Long): List[Image] = try {
//    DB.withConnection { implicit connection =>
//      SQL("""SELECT *
//             FROM Images
//             WHERE organizerId = {organizerId}""")
//        .on('organizerId -> organizerId)
//        .as(ImageParser.*)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Image.findAllByOrganizer: " + e.getMessage)
//  }
//
//  def find(imageId: Long): Option[Image] = try {
//    DB.withConnection { implicit connection =>
//      SQL("SELECT * from images WHERE placeId = {imageId}")
//        .on('imageId -> imageId)
//        .as(ImageParser.singleOpt)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Image.find: " + e.getMessage)
//  }
//
//  def save(image: Image): Option[Long] = try {
//    DB.withConnection { implicit connection =>
//      SQL(s"""SELECT exists(SELECT 1 FROM images where path = {imagePath} LIMIT 1)""")
//        .on('imagePath -> image.path)
//        .as(scalar[Boolean].single) match {
//        case true =>
//          println("Image path already in database")
//          None
//        case false =>
//          SQL("""INSERT INTO images(path, eventId, userId, placeId, organizerId)
//          VALUES({path}, {eventId}, {userId}, {placeId}, {organizerId})""")
//            .on(
//              'path -> image.path,
//              'eventId -> image.eventId,
//              'userId -> image.userId,
//              'placeId -> image.placeId,
//              'organizerId -> image.organizerId)
//            .executeInsert()
//      }
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Cannot save image: " + e.getMessage)
//  }
//
//
//  def saveEventImageRelation(eventId: Long, imageId: Long): Option[Long] = try {
//    DB.withConnection { implicit connection =>
//      SQL( """INSERT INTO eventsImages (eventId, imageId)
//          VALUES ({eventId}, {imageId})""").on(
//          'eventId -> eventId,
//          'imageId -> imageId
//        ).executeInsert()
//    }
//  } catch {
//    case e: Exception => throw new DAOException("saveEventImageRelation: " + e.getMessage)
//  }
}

