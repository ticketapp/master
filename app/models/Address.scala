package models

import anorm.SqlParser._
import anorm._
import controllers._

import play.api.Play.current
import play.api.libs.ws.{WSResponse, WS}
import services.Utilities.googleKey
import slick.model.ForeignKeyAction
import scala.language.postfixOps
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.{Success, Failure, Try}
import slick.driver.PostgresDriver.api._
import slick.model.ForeignKeyAction
import scala.language.postfixOps


case class Address (id: Option[Long],
                    geographicPoint: Option[String],
                    city: Option[String],
                    zip: Option[String],
                    street: Option[String]){
  require(!(geographicPoint.isEmpty && city.isEmpty && zip.isEmpty && street.isEmpty),
    "address must contain at least one field")
}

object Address {
  class Addresses(tag: Tag) extends Table[Address](tag, "organizers") {
    def id = column[Long]("organizerId", O.PrimaryKey)
    def geographicPoint = column[Option[String]]("geographicPoint")
    def city = column[Option[String]]("city")
    def zip = column[Option[String]]("zip")
    def street = column[Option[String]]("street")
    def * = (id.?, geographicPoint, city, zip, street) <>
      ((Address.apply _).tupled, Address.unapply)
  }

  lazy val addresses = TableQuery[Addresses]

  private val AddressParser: RowParser[Address] = {
    get[Option[String]]("geographicPoint") ~
      get[Option[String]]("city") ~
      get[Option[String]]("zip") ~
      get[Option[String]]("street") map {
      case geographicPoint ~ city ~ zip ~ street =>
        Address(None, geographicPoint, city, zip, street)
    }
  }

  def formApply(city: Option[String], zip: Option[String], street: Option[String]) =
    new Address(None, None, city, zip, street)
  def formUnapply(address: Address): Option[(Option[String], Option[String], Option[String])] =
    Some((address.city, address.zip, address.street))

  def findAll: List[Address] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM addresses").as(AddressParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Address.findAll: " + e.getMessage)
  }

  def findAllByEvent(event: Event): List[Address] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM eventsAddresses eA
          | INNER JOIN addresses a ON a.addressId = eA.addressId
          | WHERE eA.eventId = {eventId}""".stripMargin)
        .on('eventId -> event.eventId)
        .as(AddressParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Address.findAllByEvent: " + e.getMessage)
  }

  def find(addressId: Option[Long]): Option[Address] = addressId match {
    case None => None
    case Some(id) => try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM addresses WHERE addressId = {id}")
          .on('id -> id)
          .as(AddressParser.singleOpt)
      }
    } catch {
      case e: Exception => throw new DAOException("Address.find: " + e.getMessage)
    }
  }

  def findAllContaining(pattern: String): Seq[Address] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM addresses
          |  WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%'
          |LIMIT 10""".stripMargin)
        .on('patternLowCase -> pattern.toLowerCase)
        .as(AddressParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Address.findAllContaining: " + e.getMessage)
  }
  
  def save(maybeAddress: Option[Address]): Try[Option[Long]] = Try {
    maybeAddress match {
      case None =>
        None
      case Some(address) =>
        DB.withConnection { implicit connection =>
          SQL("""SELECT upsertAddress({geographicPoint}, {city}, {zip}, {street})""")
            .on(
              'geographicPoint -> address.geographicPoint,
              'city -> address.city,
              'zip -> address.zip,
              'street -> address.street)
            .as(scalar[Long].singleOpt)
        }
    }
  }

  def saveAddressInFutureWithGeoPoint(address: Option[Address]): Future[Try[Option[Long]]] = address match {
    case Some(addressWithoutGeographicPoint) if addressWithoutGeographicPoint.geographicPoint.isEmpty =>
      Address.getGeographicPoint(addressWithoutGeographicPoint) map { addressWithGeoPoint =>
        Address.save(Option(addressWithGeoPoint)) }
    case Some(addressWithGeoPoint) =>
      Future  { Address.save(Option(addressWithGeoPoint)) }
    case _ =>
      Future { Success(None) }
  }

  def saveAddressAndEventRelation(address: Address, eventId: Long): Try[Option[Long]] = save(Option(address)) match {
    case Success(Some(addressId)) => saveEventAddressRelation(eventId, addressId)
    case Success(_) => Failure(DAOException("Address.saveAddressAndEventRelation: Address.save returned None"))
    case Failure(exception) => throw exception
  }

  def saveEventAddressRelation(eventId: Long, addressId: Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO eventsAddresses (eventId, addressId)
          | VALUES ({eventId}, {addressId})""".stripMargin)
        .on(
          'eventId -> eventId,
          'addressId -> addressId)
        .executeInsert()
    }
  }

  def delete(addressId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM addresses WHERE addressId = {addressId}""")
        .on('addressId -> addressId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot delete address: " + e.getMessage)
  }

  def findGeographicPointOfCity(city: String): Option[String] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT geographicPoint FROM frenchcities WHERE name = {city}""")
        .on('city -> city)
        .as(scalar[String].singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot delete address: " + e.getMessage)
  }

  def getGeographicPoint(address: Address): Future[Address] = {
    WS.url("https://maps.googleapis.com/maps/api/geocode/json")
      .withQueryString(
        "address" -> (address.street.getOrElse("") + " " + address.zip.getOrElse("") + " " + address.city.getOrElse("")),
        "key" -> googleKey)
      .get()
      .map { response =>
      readGoogleGeographicPoint(response)  match {
        case Some(geographicPoint) => address.copy(geographicPoint = Option(geographicPoint))
        case None => address
      }
    }
  }

  def readGoogleGeographicPoint(googleGeoCodeWSResponse: WSResponse): Option[String] = {
    val googleGeoCodeJson = googleGeoCodeWSResponse.json
    val latitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lat").asOpt[BigDecimal]
    val longitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lng").asOpt[BigDecimal]
    latitude match {
      case None => None
      case Some(lat) => longitude match {
        case None => None
        case Some(lng) => Option("(" + lat + "," + lng + ")")
      }
    }
  }

  def readFacebookGeographicPoint() = {
    /*val geographicPoint = (eventJson \ "venue" \ "latitude").as[Option[Float]] match {
      case Some(latitude) =>
        (eventJson \ "venue" \ "longitude").as[Option[Float]] match {
          case Some(longitude) => Some(s"($latitude,$longitude)")
          case _ => None
        }
      case _ => None
    }*/
  }
}