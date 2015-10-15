package models

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject



import controllers.{DAOException, ThereIsNoPlaceForThisFacebookIdException}
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class Place (id: Option[Long],
                  name: String,
                  facebookId: Option[String] = None,
                  geographicPoint: Option[String],
                  description: Option[String] = None,
                  webSites: Option[String] = None,
                  capacity: Option[Int] = None,
                  openingHours: Option[String] = None,
                  imagePath: Option[String] = None,
                  /*address : Option[Address] = None,*/
                  linkedOrganizerId: Option[Long] = None)

class PlaceMethods @Inject()(dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val utilities: Utilities) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._

  implicit def dateTime = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime))

  def formApply(name: String, facebookId: Option[String], geographicPoint: Option[String], description: Option[String],
                webSite: Option[String], capacity: Option[Int], openingHours: Option[String],
                imagePath: Option[String], city: Option[String], zip: Option[String], street: Option[String]): Place = {
    try {
      val address = Option(Address(None, None, city, zip, street))
      new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath/*, address*/)
    } catch {
      case e: IllegalArgumentException =>
        new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath, None)
    }
  }

  class Places(tag: Tag) extends Table[Place](tag, "places") {
    def id = column[Long]("placeid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def facebookId = column[Option[String]]("facebookid")
    def geographicPoint = column[Option[String]]("geographicpoint")
    def description = column[Option[String]]("description")
    def websites = column[Option[String]]("websites")
    def capacity = column[Option[Int]]("capacity")
    def openingHours = column[Option[String]]("openinghours")
    def imagePath = column[Option[String]]("imagepath")
    def linkedOrganizerId = column[Option[Long]]("linkedorganizerid")

    def * = (id.?, name, facebookId, geographicPoint, description, websites, capacity, openingHours,
      imagePath, linkedOrganizerId) <> ((Place.apply _).tupled, Place.unapply)
  }

  lazy val places = TableQuery[Places]

//  def formApply(name: String, facebookId: Option[String], geographicPoint: Option[String], description: Option[String],
//                webSite: Option[String], capacity: Option[Int], openingHours: Option[String],
//                imagePath: Option[String], city: Option[String], zip: Option[String], street: Option[String]): Place = {
//    try {
//      val address = Option(Address(None, None, city, zip, street))
//      new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath, address)
//    } catch {
//      case e: IllegalArgumentException =>
//        new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath, None)
//    }
//  }
//
//  def formUnapply(place: Place) =
//    Some((place.name, place.facebookId, place.geographicPoint, place.description, place.webSites, place.capacity,
//      place.openingHours, place.imagePath, place.address.get.city, place.address.get.zip, place.address.get.street))
//
//  private val PlaceParser: RowParser[Place] = {
//    get[Long]("placeId") ~
//      get[String]("name") ~
//      get[Option[String]]("facebookId") ~
//      get[Option[String]]("geographicPoint") ~
//      get[Option[String]]("description") ~
//      get[Option[String]]("webSites") ~
//      get[Option[Int]]("capacity") ~
//      get[Option[String]]("openingHours") ~
//      get[Option[Long]]("addressId") ~
//      get[Option[String]]("imagePath") ~
//      get[Option[Long]]("organizerId") map {
//      case placeId ~ name ~ facebookId ~ geographicPoint ~ description ~ webSites ~ capacity ~ openingHours ~
//        addressId  ~ imagePath ~ organizerId =>
//          Place(Option(placeId), name, facebookId, geographicPoint, description, webSites, capacity, openingHours,
//            imagePath, Address.find(addressId), organizerId)
//    }
//  }

  def delete(id: Long): Future[Int] = db.run(places.filter(_.id === id).delete)

  def save(place: Place): Future[Place] = {
//      val eventuallyAddressId = Address.saveAddressInFutureWithGeoPoint(place.address)
    val query = places returning places.map(_.id) into ((place, id) => place.copy(id = Option(id))) +=
      place.copy(description =  utilities.formatDescription(place.description))

    db.run(query)
  }

//  def save(place: Place): Future[Try[Option[Long]]] = {
//    val eventuallyAddressId = Address.saveAddressInFutureWithGeoPoint(place.address)
//    eventuallyAddressId map {
//      case Success(addressId) =>
//        val description = Utilities.formatDescription(place.description)
//        DB.withConnection { implicit connection =>
//          Try {
//            SQL(
//              s"""SELECT insertPlace({name}, {geographicPoint}, {addressId}, {facebookId}, {description},
//                 |{webSites}, {capacity}, {openingHours}, {imagePath}, {organizerId})""".stripMargin)
//              .on(
//                'name -> place.name,
//                'geographicPoint -> place.geographicPoint,
//                'addressId -> addressId,
//                'facebookId -> place.facebookId,
//                'description -> description,
//                'webSites -> Utilities.setToOptionString(getNormalizedWebsitesInText(place.webSites)),
//                'capacity -> place.capacity,
//                'openingHours -> place.openingHours,
//                'imagePath -> place.imagePath,
//                'organizerId -> organizerMethods.findIdByFacebookId(place.facebookId))
//              .as(scalar[Long].singleOpt)
//          }
//        }
//      case Failure(e) =>
//        Logger.error("Place.save: ", e)
//        throw e
//    }
//  }
/*
  def findIdByFacebookId(placeFacebookId: Option[String]): Future[Option[Long]] = {
    val query =
=======
  def save(place: Place): Future[Try[Option[Long]]] = {
    val eventuallyAddressId = Address.saveAddressInFutureWithGeoPoint(place.address)
    eventuallyAddressId map {
      case Success(addressId) =>
        val description = Utilities.formatDescription(place.description)
        DB.withConnection { implicit connection =>
          Try {
            SQL(
              s"""SELECT insertPlace({name}, {geographicPoint}, {addressId}, {facebookId}, {description},
                 |{webSites}, {capacity}, {openingHours}, {imagePath}, {organizerId})""".stripMargin)
              .on(
                'name -> place.name,
                'geographicPoint -> place.geographicPoint,
                'addressId -> addressId,
                'facebookId -> place.facebookId,
                'description -> description,
                'webSites -> Utilities.setToOptionString(getNormalizedWebsitesInText(place.webSites)),
                'capacity -> place.capacity,
                'openingHours -> place.openingHours,
                'imagePath -> place.imagePath,
                'organizerId -> organizerMethods.findIdByFacebookId(place.facebookId))
              .as(scalar[Long].singleOpt)
          }
        }
      case Failure(e) =>
        Logger.error("Place.save: ", e)
        throw e
    }
  }

<<<<<<< HEAD

  def getPlaceByFacebookId(placeFacebookId : Option[String]) : Future[Option[Place]] = {
    findIdByFacebookId(placeFacebookId) match {
      case Some(id) =>
        find(id) match {
          case Some(place) =>
            Future { Option(place) }
          case None =>
            getPlaceOnFacebook(placeFacebookId)
        }
      case None =>
        getPlaceOnFacebook(placeFacebookId)
    }
  }

  def getPlaceOnFacebook(placeFacebookId: Option[String]): Future[Option[Place]] = placeFacebookId match {
    case Some(id) =>
      WS.url("https://graph.facebook.com/" + Utilities.facebookApiVersion +"/" + id)
        .withQueryString(
          "fields" -> "about,location,website,hours,cover,name",
          "access_token" -> facebookToken)
        .get()
        .flatMap { readFacebookPlace }
    case None =>
      Logger.error("Place.getPlaceOnFacebook: no placeFacebookId")
      Future { None }
  }

  def readFacebookPlace (placeFacebookResponse: Response): Future[Option[Place]] = {
    val placeRead = (
      (__ \ "about").readNullable[String] and
        (__ \ "cover" \ "source").readNullable[String] and
        (__ \ "name").read[String] and
        (__ \ "id").readNullable[String] and
        (__ \ "location" \ "street").readNullable[String] and
        (__ \ "location" \ "zip").readNullable[String] and
        (__ \ "location" \ "city").readNullable[String] and
        (__ \ "location" \ "country").readNullable[String] and
        (__ \ "website").readNullable[String]
      ).apply((about: Option[String], source: Option[String], name: String, facebookId: Option[String],
               street: Option[String], zip: Option[String], city: Option[String], country: Option[String],
               website: Option[String]) => {
      val address = Address(None, None, city, zip, street)
      val newPlace = Place(None, name, facebookId, None, about, website, None, None, source, Option(address))
      save(newPlace) map {
        case Success(Some(id)) =>
          find(id)
        case _ =>
          Logger.error("Place.readFacebookPlace: place could not be saved")
          None
      }
    })

    placeFacebookResponse.json.as[Future[Option[Place]]](placeRead)
  }

  def findIdByFacebookId(placeFacebookId: Option[String]): Option[Long] = DB.withConnection { implicit connection =>
    SQL(
      """SELECT placeId FROM places
=======
  def findIdByFacebookId(placeFacebookId: Option[String]): Option[Long] = {
>>>>>>> master
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT placeId FROM places
>>>>>>> Play2.4AndSlickMigrationResolvingConflicts
        | WHERE facebookId = {facebookId}""".stripMargin)
        .on("facebookId" -> placeFacebookId)
        .as(scalar[Long].singleOpt)
    }
  }

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM places
           |  ORDER BY geographicPoint <-> point '$geographicPoint'
           |LIMIT $numberToReturn
           |OFFSET $offset""".stripMargin)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findNear: " + e.getMessage)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Seq[Place] = try {
    Address.findGeographicPointOfCity(city) match {
      case None => Seq.empty
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findNearCity: " + e.getMessage)
  }
  
  def findAll: Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places")
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAll: " + e.getMessage)
  }

  def findAllByEvent(eventId: Long): List[Place] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM eventsPlaces eA
          |INNER JOIN places a ON a.placeId = eA.placeId
          |WHERE eA.eventId = {eventId}""".stripMargin)
        .on('eventId -> eventId)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAllByEvent: " + e.getMessage)
  }

  def findAllWithFacebookId: Try[List[Place]] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT *
          |  FROM places 
          |  WHERE facebookId IS NOT NULL""".stripMargin)
        .as(PlaceParser.*)
    }
  }

  def findAllContaining(pattern: String): Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 5")
        .on('patternLowCase -> pattern.toLowerCase)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAllContaining: " + e.getMessage)
  }

  def find(placeId: Long): Option[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places WHERE placeId = {placeId}")
        .on('placeId -> placeId)
        .as(PlaceParser.singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.find: " + e.getMessage)
  }

  def findIdByFacebookId(facebookId: String): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT placeId FROM places WHERE facebookId = {facebookId}""")
        .on('facebookId -> facebookId)
        .as(scalar[Long].singleOpt)
    }
  }

  def followByPlaceId(userId : UUID, placeId : Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO placesFollowed(userId, placeId) VALUES({userId}, {placeId})""")
        .on(
          'userId -> userId,
          'placeId -> placeId)
        .executeInsert()
    }
  }

  def unfollowByPlaceId(userId: UUID, placeId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM placesFollowed
          | WHERE userId = {userId} AND placeId = {placeId}""".stripMargin)
        .on('userId -> userId,
          'placeId -> placeId)
        .executeUpdate()
    }
  }

  def followByFacebookId(userId : UUID, facebookId: String): Try[Option[Long]] =
    findIdByFacebookId(facebookId) match {
      case Success(Some(placeId)) => followByPlaceId(userId, placeId)
      case Success(None)=> Failure(ThereIsNoPlaceForThisFacebookIdException("Place.followByFacebookId"))
      case failure => failure
    }

  def isFollowed(userId: UUID, placeId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM placesFollowed
          |  WHERE userId = {userId} AND placeId = {placeId})""".stripMargin)
        .on("userId" -> userId,
          "placeId" -> placeId)
        .as(scalar[Boolean].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.isPlaceFollowed: " + e.getMessage)
  }

  def getFollowedPlaces(userId: UUID): Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("""select a.* from places a
            |  INNER JOIN placesFollowed af ON a.placeId = af.placeId
            |WHERE af.userId = {userId}""".stripMargin)
        .on('userId -> userId)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.getFollowedPlaces: " + e.getMessage)
  }

  def saveEventRelation(eventId: Long, placeId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertEventPlaceRelation({eventId}, {placeId})""")
        .on(
          'eventId -> eventId,
          'placeId -> placeId)
        .execute()
    }
  } catch {
    case e: Exception =>
      Logger.error(s"Place.saveEventRelation: error with eventId $eventId and placeId $placeId", e)
      throw new DAOException("Place.saveEventRelation: " + e.getMessage)
  }
  
  def deleteEventRelation(eventId: Long, placeId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(s"""DELETE FROM eventsPlaces WHERE eventId = $eventId AND placeId = $placeId""")
        .executeUpdate()
    }
  }*/
}
