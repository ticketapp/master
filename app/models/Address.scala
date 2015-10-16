package models

import javax.inject.Inject

import com.vividsolutions.jts.geom.Point
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}

import scala.concurrent.Future
import scala.language.postfixOps


case class Address (id: Option[Long],
                    geographicPoint: Option[String],
                    city: Option[String],
                    zip: Option[String],
                    street: Option[String]){
  require(!(geographicPoint.isEmpty && city.isEmpty && zip.isEmpty && street.isEmpty),
    "address must contain at least one field")
}

class AddressMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                              val eventMethods: EventMethods,
                              val utilities: Utilities) extends HasDatabaseConfigProvider[MyPostgresDriver] {

  val events = eventMethods.events
  val eventsAddresses = eventMethods.eventsAddresses

  class Addresses(tag: Tag) extends Table[Address](tag, "addresses") {
    def id = column[Long]("organizerId", O.PrimaryKey)
    def geographicPoint = column[Option[String]]("geographicPoint")
    def city = column[Option[String]]("city")
    def zip = column[Option[String]]("zip")
    def street = column[Option[String]]("street")
    def * = (id.?, geographicPoint, city, zip, street) <>
      ((Address.apply _).tupled, Address.unapply)
  }

  lazy val addresses = TableQuery[Addresses]

  case class FrenchCity(city: String, geographicPoint: Point)

  class FrenchCities(tag: Tag) extends Table[FrenchCity](tag, "frenchcities") {
    def id = column[Long]("cityid", O.PrimaryKey)
    def city = column[String]("city")
    def geographicPoint = column[Point]("geographicpoint")
    def * = (city, geographicPoint) <> ((FrenchCity.apply _).tupled, FrenchCity.unapply)
  }

  lazy val frenchCities = TableQuery[FrenchCities]

  def formApply(city: Option[String], zip: Option[String], street: Option[String]) =
    new Address(None, None, city, zip, street)
  def formUnapply(address: Address): Option[(Option[String], Option[String], Option[String])] =
    Some((address.city, address.zip, address.street))

  def findAll: Future[Seq[Address]] = db.run(addresses.result)

  def findAllByEvent(event: Event): Future[Seq[Address]] = {
    val query = for {
      event <- events if event.id === event.id
      eventAddress <- eventsAddresses
      address <- addresses if address.id === eventAddress.addressId
    } yield address

    db.run(query.result)
  }

  def find(id: Long): Future[Option[Address]] = db.run(addresses.filter(_.id === id).result.headOption)

  def findAllContaining(cityPattern: String): Future[Seq[Address]] = {
    val lowercasePattern = cityPattern.toLowerCase
    val query = for {
      address <- addresses if address.city.toLowerCase like s"%$lowercasePattern%"
    } yield address

    db.run(query.result)
  }

  def save(address: Address): Future[Address] = {
//    val query3 = for {
//      existing <- addresses
//        .filter(a => a.street === address.street && a.zip === address.zip && a.city === address.city)
//        .result
//        .headOption
//      row = existing getOrElse address
//      result <- (addresses returning addresses.map(_.id) into ((address, id) => address.copy(id = Some(id)))).insertOrUpdate(row)
//      toBeReturned <- result getOrElse existing.get
//    } yield toBeReturned
//
//    db.run(query3)
//


/*    val query = (addresses returning addresses.map(_.id) into ((address, id) => address.copy(id = Some(id)))) insertOrUpdate address
    db.run(query)
*/
    val query2 = (addresses returning addresses.map(_.id)) insertOrUpdate address
    db.run(query2) flatMap {
      case None =>
        db.run(addresses
          .filter(a => a.street === address.street && a.zip === address.zip && a.city === address.city)
          .result
          .head)
      case Some(addressId) =>
        Future { address.copy(id = Option(addressId)) }
    }
  }

  def saveAddressInFutureWithGeoPoint(address: Address): Future[Address] = address match {
    case addressWithoutGeographicPoint if addressWithoutGeographicPoint.geographicPoint.isEmpty =>
      getGeographicPoint(addressWithoutGeographicPoint) flatMap { save }
    case addressWithGeoPoint =>
      save(addressWithGeoPoint)
  }

  def saveAddressAndEventRelation(address: Address, eventId: Long): Future[Int] = save(address) flatMap { a =>
    a.id match {
      case None =>
        Logger.error("Address.saveAddressAndEventRelation: address saved did not return an id")
        Future(0)
      case Some(id) =>
      saveEventAddressRelation(eventId, id)
    }
  }

  def saveEventAddressRelation(eventId: Long, addressId: Long): Future[Int] =
    db.run(eventsAddresses += ((eventId, addressId)))

  def delete(id: Long): Future[Int] = db.run(addresses.filter(_.id === id).delete)

  def findGeographicPointOfCity(city: String): Future[Option[Point]] = {
    val query = frenchCities.filter(_.city === city) map (_.geographicPoint)
    db.run(query.result.headOption)
  }

  def getGeographicPoint(address: Address): Future[Address] = {
    WS.url("https://maps.googleapis.com/maps/api/geocode/json")
      .withQueryString(
        "address" -> (address.street.getOrElse("") + " " + address.zip.getOrElse("") + " " + address.city.getOrElse("")),
        "key" -> utilities.googleKey)
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
//   val geographicPoint = (eventJson \ "venue" \ "latitude").as[Option[Float]] match {
//     case Some(latitude) =>
//       (eventJson \ "venue" \ "longitude").as[Option[Float]] match {
//         case Some(longitude) => Some(s"($latitude,$longitude)")
//         case _ => None
//       }
//     case _ => None
//   }
 }
}