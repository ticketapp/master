package models

import javax.inject.Inject

import controllers.OverQueryLimit
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


case class Address (id: Option[Long],
                    geographicPoint: Option[String],
                    city: Option[String],
                    zip: Option[String],
                    street: Option[String]){
  require(!(geographicPoint.isEmpty && city.isEmpty && zip.isEmpty && street.isEmpty),
    "address must contain at least one field")
}

class AddressMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                               val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {



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

  def saveAddressWithGeoPoint(address: Address): Future[Address] = address match {
    case addressWithoutGeographicPoint if addressWithoutGeographicPoint.geographicPoint.isEmpty =>
      getGeographicPoint(addressWithoutGeographicPoint, retry = 3) flatMap { save }
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

  def getGeographicPoint(address: Address, retry: Int): Future[Address] = WS
    .url("https://maps.googleapis.com/maps/api/geocode/json")
    .withQueryString(
      "address" -> (address.street.getOrElse("") + " " + address.zip.getOrElse("") + " " + address.city.getOrElse("")),
      "key" -> utilities.googleKey)
    .get()
    .flatMap { readGoogleGeographicPoint(_) match {
      case Success(Some(geographicPoint)) =>
        Future { address.copy(geographicPoint = Option(geographicPoint)) }
      case Success(None) =>
        Future { address }
      case Failure(e: OverQueryLimit) if retry > 0 =>
        Logger.info("Address.getGeographicPoint: retry: " + retry + " ", e)
        getGeographicPoint(address, retry - 1)
      case Failure(e: Exception) =>
        Logger.error("Address.getGeographicPoint: ", e)
        Future { address }
    }
  }

  def readGoogleGeographicPoint(googleGeoCodeResponse: WSResponse): Try[Option[String]] = {
    googleGeoCodeResponse.statusText match {
      case "OK" =>
        val googleGeoCodeJson = googleGeoCodeResponse.json
        val latitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lat").asOpt[BigDecimal]
        val longitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lng").asOpt[BigDecimal]
        latitude match {
          case None => Success(None)
          case Some(lat) => longitude match {
            case None => Success(None)
            case Some(lng) => Success(Option("(" + lat + "," + lng + ")"))
          }
        }
      case "OVER_QUERY_LIMIT" =>
        Failure(OverQueryLimit("Address.readGoogleGeographicPoint"))
      case otherStatus =>
        Failure(new Exception(otherStatus))
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