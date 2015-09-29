package models

import java.util.UUID
import javax.inject.Inject

import controllers.{DAOException, ThereIsNoOrganizerForThisFacebookIdException}
import models.Address.addresses
import play.api.Play.current
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.Utilities
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.model.ForeignKeyAction

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try

case class Organizer (id: Option[Long],
                      facebookId: Option[String] = None,
                      name: String,
                      description: Option[String] = None,
                      addressId: Option[Long] = None,
                      phone: Option[String] = None,
                      publicTransit: Option[String] = None,
                      websites: Option[String] = None,
                      verified: Boolean = false,
                      imagePath: Option[String] = None,
                      geographicPoint: Option[String] = None,
                      linkedPlaceId: Option[Long] = None)

case class OrganizerWithAddress(organizer: Organizer, address: Option[Address])


class OrganizerMethods @Inject()(dbConfigProvider: DatabaseConfigProvider,
                                 val placeMethods: PlaceMethods,
                                 val utilities: Utilities) {
    val dbConfig = dbConfigProvider.get[JdbcProfile]
    import dbConfig._

  class Organizers(tag: Tag) extends Table[Organizer](tag, "organizers") {
    def id = column[Long]("organizerid", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def addressId = column[Option[Long]]("addressid")
    def phone = column[Option[String]]("phone")
    def publicTransit = column[Option[String]]("publictransit")
    def websites = column[Option[String]]("websites")
    def verified = column[Boolean]("verified")
    def imagePath = column[Option[String]]("imagepath")
    def geographicPoint = column[Option[String]]("geographicpoint")
    def linkedPlaceId = column[Option[Long]]("placeid")

    def * = (id.?, facebookId, name, description, addressId, phone, publicTransit, websites, verified, imagePath, geographicPoint, linkedPlaceId) <>
      ((Organizer.apply _).tupled, Organizer.unapply)

    def address = foreignKey("addressFk", addressId, addresses)(_.id.?, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val organizers = TableQuery[Organizers]

  def formApply(facebookId: Option[String], name: String, description: Option[String], websites: Option[String],
                imagePath: Option[String]): Organizer =
    Organizer(None, facebookId, name, description = description, websites = websites, imagePath = imagePath)
  def formUnapply(organizer: Organizer) =
    Some((organizer.facebookId, organizer.name, organizer.description, organizer.websites, organizer.imagePath))

//  def getOrganizerProperties(organizer: Organizer): Organizer = organizer.copy(
//    address = Address.find(organizer.addressId)
//  )

  def find(numberToReturn: Int, offset: Int): Future[Seq[OrganizerWithAddress]] = {
    val tupledJoin = organizers joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result).map(_.map(OrganizerWithAddress.tupled))
  }


  def findAllByEvent(event: Event): Future[Seq[Organizer]] = {
    Future { Seq.empty }
  }
    /*
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM eventsOrganizers eA
          | INNER JOIN organizers a ON a.organizerId = eA.organizerId
          | WHERE eA.eventId = {eventId}""".stripMargin)
        .on('eventId -> event.eventId)
        .as(OrganizerParser.*)
//        .map(getOrganizerProperties)
    }
  } */

  def findById(id: Long): Future[Option[Organizer]] = {
    val query = organizers.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def findAllContaining(pattern: String): Future[Seq[Organizer]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      organizer <- organizers if organizer.name.toLowerCase like s"%$lowercasePattern%"
    } yield organizer

    db.run(query.result)
  }

//      SQL("SELECT * FROM organizers WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
//        .on('patternLowCase -> pattern.toLowerCase)
//        .as(OrganizerParser.*)
////        .map(getOrganizerProperties)


  def save(organizer: Organizer): Future[Organizer] = {
    val insertQuery = organizers returning organizers.map(_.id) into ((organizer, id) =>
      organizer.copy(id = Option(id)))

    val action = insertQuery += organizer

    db.run(action)
  }

//  def save(organizer: Organizer): Future[Long] = {
////    val addressId = organizer.address match {
////      case None => None
////      case Some(address) => Address.save(Option(address))
////    }
//    val placeIdWithSameFacebookId = placeMethods.findIdByFacebookId(organizer.facebookId)
//    val phoneNumbers = Utilities.phoneNumbersSetToOptionString(Utilities.phoneNumbersStringToSet(organizer.phone))
//    val description = Utilities.formatDescription(organizer.description)
//
//    val query = organizers.map(organizer => (organizer.facebookId, organizer.name,
//      organizer.description, organizer.addressId, organizer.phone, organizer.publicTransit, organizer.websites,
//      organizer.verified, organizer.imagePath, organizer.geographicPoint, organizer.linkedPlaceId)) += // returning organizers.map(_.organizerId) +=
//      (organizer.facebookId, organizer.name, description, organizer.addressId,
//        phoneNumbers, organizer.publicTransit, organizer.websites, organizer.verified,
//        organizer.imagePath, organizer.geographicPoint, placeIdWithSameFacebookId)
//
//    db.run(query) map {
////      case res: Long => res
////      case _ => println("nique") ; 0
//      case _ => 0
//    }
//
////    DB.withConnection { implicit connection =>
////      val addressId = organizer.address match {
////        case None => None
////        case Some(address) => Address.save(Option(address))
////      }
////      val placeIdWithSameFacebookId = Place.findIdByFacebookId(organizer.facebookId)
////      val phoneNumbers = Utilities.phoneNumbersSetToOptionString(Utilities.phoneNumbersStringToSet(organizer.phone))
////      val description = Utilities.formatDescription(organizer.description)
////      SQL(
////        """SELECT insertOrganizer({facebookId}, {name}, {description}, {addressId}, {phone}, {publicTransit},
////          |{websites}, {imagePath}, {geographicPoint}, {placeId})""".stripMargin)
////        .on(
////          'facebookId -> organizer.facebookId,
////          'name -> organizer.name,
////          'description -> description,
////          'addressId -> addressId,
////          'phone -> phoneNumbers,
////          'publicTransit -> organizer.publicTransit,
////          'websites -> organizer.websites,
////          'imagePath -> organizer.imagePath,
////          'geographicPoint -> organizer.geographicPoint,
////          'placeId -> placeIdWithSameFacebookId)
////        .as(scalar[Option[Long]].single)
////    }
//  }

  def findIdByFacebookId(facebookId: Option[String]): Future[Option[Long]] = {
    val query = organizers.filter(_.facebookId === facebookId).map(_.id)
    db.run(query.result.headOption)
  }

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): Future[Seq[Organizer]] = {
    Future { Seq.empty }
//    DB.withConnection { implicit connection =>
//      SQL(
//        s"""SELECT * FROM organizers
//           |ORDER BY geographicPoint <-> point '$geographicPoint'
//                                                                  |LIMIT $numberToReturn
//            |OFFSET $offset""".stripMargin)
//        .as(OrganizerParser.*)
////        .map(getOrganizerProperties)
//    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Organizer]] = {
    Address.findGeographicPointOfCity(city) match {
      case None => Future { Seq.empty }
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
    }
  } 

  def saveWithEventRelation(organizer: Organizer, eventId: Long): Boolean = {
//    save(organizer) map {
//      case Success(Some(organizerId)) => saveEventRelation(eventId, organizerId)
//      case Success(None) => false
//      case Failure(_) => false
//    }
    true
  }

  def saveEventRelation(eventId: Long, organizerId: Long): Boolean = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT insertEventOrganizerRelation({eventId}, {organizerId})""")
        .on(
          'eventId -> eventId,
          'organizerId -> organizerId)
        .execute()
    }
  } 

//  def deleteEventRelation(eventId: Long, organizerId: Long): Try[Int] = Try {
//    DB.withConnection { implicit connection =>
//      SQL(s"""DELETE FROM eventsOrganizers WHERE eventId = $eventId AND organizerId = $organizerId""")
//        .executeUpdate()
//    }
//  }

  def delete(id: Long): Future[Int] = {
    val query = organizers.filter(_.id === id)
    db.run(query.delete)
  }

  def followById(userId : UUID, organizerId : Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO organizersFollowed(userId, organizerId)
          |VALUES({userId}, {organizerId})""".stripMargin)
        .on(
          'userId -> userId,
          'organizerId -> organizerId)
        .executeInsert()
    }
  }

  def followByFacebookId(userId : UUID, facebookId: String): Try[Option[Long]] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT organizerId FROM organizers WHERE facebookId = {facebookId}""")
        .on('facebookId -> facebookId)
        .as(scalar[Long].singleOpt) match {
        case None => throw new ThereIsNoOrganizerForThisFacebookIdException("Organizer.followOrganizerIdByFacebookId")
        case Some(organizerId) => followById(userId, organizerId)
      }
    }
  }

  def unfollowByOrganizerId(userId: UUID, organizerId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM organizersFollowed
          | WHERE userId = {userId} AND organizerId = {organizerId}""".stripMargin)
        .on('userId -> userId,
          'organizerId -> organizerId)
        .executeUpdate()
    }
  }

  def isFollowed(userId: UUID, organizerId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM organizersFollowed
          |  WHERE userId = {userId} AND organizerId = {organizerId})""".stripMargin)
        .on("userId" -> userId,
          "organizerId" -> organizerId)
        .as(scalar[Boolean].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.isOrganizerFollowed: " + e.getMessage)
  }

  def getFollowedOrganizers(userId: UUID): Seq[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL("""select a.* from organizers a
            |  INNER JOIN organizersFollowed af ON a.organizerId = af.organizerId
            |WHERE af.userId = {userId}""".stripMargin)
        .on('userId -> userId)
        .as(OrganizerParser.*)
//        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.getFollowedOrganizers: " + e.getMessage)
  }

  def readOrganizer(organizer: WSResponse, organizerId: String): Option[OrganizerWithAddress] = {
    val readOrganizer = (
      (__ \ "name").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "cover" \ "source").readNullable[String] and
        (__ \ "location" \ "street").readNullable[String] and
        (__ \ "location" \ "zip").readNullable[String] and
        (__ \ "location" \ "city").readNullable[String] and
        (__ \ "phone").readNullable[String] and
        (__ \ "public_transit").readNullable[String] and
        (__ \ "website").readNullable[String])
      .apply((name: String, description: Option[String], source: Option[String], street: Option[String],
              zip: Option[String], city: Option[String], phone: Option[String], public_transit: Option[String],
              website: Option[String]) =>
      OrganizerWithAddress(Organizer(None, Some(organizerId), name, utilities.formatDescription(description), None, phone, public_transit,
        website, verified = false, imagePath = source, geographicPoint = None), address = Option(Address(None, None,
          city, zip, street)))
      )
    organizer.json.asOpt[OrganizerWithAddress](readOrganizer)
  }

  def getOrganizerInfo(maybeOrganizerId: Option[String]): Future[Option[OrganizerWithAddress]] = maybeOrganizerId match {
    case None => Future { None }
    case Some(organizerId) =>
      WS.url("https://graph.facebook.com/v2.2/" + organizerId)
        .withQueryString(
          "fields" -> "name,description,cover{source},location,phone,public_transit,website",
          "access_token" -> facebookToken)
        .get()
        .map { response => readOrganizer(response, organizerId) }
  }
}