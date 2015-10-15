package models

import javax.inject.Inject

import com.vividsolutions.jts.geom.Point
import json.JsonHelper._
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}
import slick.model.ForeignKeyAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

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
                      geographicPoint: Option[Point] = None,
                      linkedPlaceId: Option[Long] = None)

case class OrganizerWithAddress(organizer: Organizer, address: Option[Address])

case class OrganizerFollowed(userId: String, Organizer: Long)

class OrganizerMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                 val placeMethods: PlaceMethods,
                                 val eventMethods: EventMethods,
                                 val utilities: Utilities,
                                 val addressMethods: AddressMethods) extends HasDatabaseConfigProvider[MyPostgresDriver] {

  val addresses = addressMethods.addresses
  val events = eventMethods.events
  val eventsOrganizers = eventMethods.eventsOrganizers
  import eventMethods.EventOrganizer

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
    def geographicPoint = column[Option[Point]]("geographicpoint")
    def linkedPlaceId = column[Option[Long]]("placeid")

    def * = (id.?, facebookId, name, description, addressId, phone, publicTransit, websites, verified, imagePath, geographicPoint, linkedPlaceId) <>
      ((Organizer.apply _).tupled, Organizer.unapply)

    def address = foreignKey("addressFk", addressId, addresses)(_.id.?, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val organizers = TableQuery[Organizers]

  class OrganizersFollowed(tag: Tag) extends Table[OrganizerFollowed](tag, "organizersfollowed") {
    def userId = column[String]("userid")
    def organizerId = column[Long]("organizerid")

    def * = (userId, organizerId) <> ((OrganizerFollowed.apply _).tupled, OrganizerFollowed.unapply)
  }

  lazy val organizersFollowed = TableQuery[OrganizersFollowed]

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
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      event <- events if event.id === event.id
      eventOrganizer <- eventsOrganizers
      organizer <- organizers if organizer.id === eventOrganizer.organizerId
    } yield organizer

    //getOrganizerProperties
    db.run(query.result)
  }

  def findById(id: Long): Future[Option[Organizer]] = {
    val query = organizers.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def findAllContaining(pattern: String): Future[Seq[Organizer]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      organizer <- organizers if organizer.name.toLowerCase like s"%$lowercasePattern%"
    } yield organizer
    //        .map(getOrganizerProperties)
    db.run(query.result)
  }

  def save(organizer: Organizer): Future[Organizer] = {
    val insertQuery = organizers returning organizers.map(_.id) into ((organizer, id) => organizer.copy(id = Option(id)))
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
//    }
//  }

  def findIdByFacebookId(facebookId: Option[String]): Future[Option[Long]] = {
    val query = organizers.filter(_.facebookId === facebookId).map(_.id)
    db.run(query.result.headOption)
  }

  def findNear(geographicPoint: Point, numberToReturn: Int, offset: Int): Future[Seq[Organizer]] = {
    val query = organizers
      .sortBy(_.geographicPoint <-> geographicPoint)
      .drop(numberToReturn)
      .take(offset)
    db.run(query.result)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Organizer]] =
    addressMethods.findGeographicPointOfCity(city) flatMap {
      case None =>
        Logger.info("Organizer.findNearCity: no city found with this name")
        Future { Seq.empty }
      case Some(geographicPoint) =>
        findNear(geographicPoint, numberToReturn, offset)
  }


  def saveWithEventRelation(organizer: Organizer, eventId: Long): Future[Int] = save(organizer) flatMap {
    case organizer: Organizer => saveEventRelation(eventId, organizer.id.get)
  }

  def saveEventRelation(eventId: Long, organizerId: Long): Future[Int] =
    db.run(eventsOrganizers += EventOrganizer(eventId, organizerId))


  def deleteEventRelation(eventId: Long, organizerId: Long): Future[Int] = db.run(eventsOrganizers.filter(eventOrganizer =>
      eventOrganizer.eventId === eventId && eventOrganizer.organizerId === organizerId).delete)

    def delete(id: Long): Future[Int] = db.run(organizers.filter(_.id === id).delete)
  /*
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
*/
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
         "access_token" -> utilities.facebookToken)
       .get()
       .map { response => readOrganizer(response, organizerId) }
    }
}