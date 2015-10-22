package models

import javax.inject.Inject

import com.vividsolutions.jts.geom.{Geometry, Point}
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
                    geographicPoint: Option[Geometry],
                    city: Option[String],
                    zip: Option[String],
                    street: Option[String]){
  require(!(geographicPoint.isEmpty && city.isEmpty && zip.isEmpty && street.isEmpty),
    "address must contain at least one field")
}

class AddressMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                               val utilities: Utilities,
                               val geographicPointMethods: GeographicPointMethods)
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
    val lowerCaseAddress = address.copy(id = address.id, geographicPoint = address.geographicPoint,
      street = utilities.optionStringToLowerCaseOptionString(address.street), zip = address.zip,
      city = utilities.optionStringToLowerCaseOptionString(address.city))

    db.run((for {
        addressFound <- addresses.filter(a => a.street === lowerCaseAddress.street &&
          a.zip === lowerCaseAddress.zip && a.city === lowerCaseAddress.city).result.headOption
        result <- addressFound.map(DBIO.successful).getOrElse(addresses returning addresses.map(_.id) += lowerCaseAddress)
      } yield result match {
        case addressWithoutGeographicPoint: Address
          if addressWithoutGeographicPoint.geographicPoint.isEmpty && lowerCaseAddress.geographicPoint.nonEmpty =>
            val updatedAddress = lowerCaseAddress.copy(id = addressWithoutGeographicPoint.id)
            update(updatedAddress) map {
              case int if int != 1 =>
                Logger.error("Address.save: not exactly one row was updated")
                addressWithoutGeographicPoint
              case _ =>
                updatedAddress
            }
        case a: Address =>
          Future(a)
        case id: Long =>
          Future(lowerCaseAddress.copy(id = Option(id)))
        }).transactionally).flatMap(eventuallyAddress => eventuallyAddress)
  }

  def update(address: Address): Future[Int] = db.run(addresses.filter(_.id === address.id).update(address))

  def saveAddressWithGeoPoint(address: Address): Future[Address] = address match {
    case addressWithoutGeographicPoint if addressWithoutGeographicPoint.geographicPoint.isEmpty =>
      geographicPointMethods.getGeographicPoint(addressWithoutGeographicPoint, retry = 3) flatMap { save }
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
}