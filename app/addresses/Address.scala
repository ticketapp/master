package addresses

import javax.inject.Inject

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Geometry}
import database.{MyPostgresDriver, EventAddressRelation, MyDBTableDefinitions}
import eventsDomain.Event
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import MyPostgresDriver.api._
import services.Utilities

import scala.concurrent.Future
import scala.language.postfixOps


case class Address(id: Option[Long] = None,
                   geographicPoint: Geometry = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
                   city: Option[String] = None,
                   zip: Option[String] = None,
                   street: Option[String] = None) {
  require(
    !(geographicPoint == new GeometryFactory().createPoint(new Coordinate(-84, 30)) && city.isEmpty &&
      zip.isEmpty && street.isEmpty),
    "address must contain at least one field")
}

class AddressMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                               val searchGeographicPoint: SearchGeographicPoint)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with AddressFormsTrait
    with Utilities {

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
    val lowerCaseAddress = address.copy(
      street = optionStringToLowerCaseOptionString(address.street),
      city = optionStringToLowerCaseOptionString(address.city))

    db.run(
      (for {
        addressFound <- addresses.filter(a => a.street === lowerCaseAddress.street &&
          a.zip === lowerCaseAddress.zip && a.city === lowerCaseAddress.city).result.headOption
        result <- addressFound.map(DBIO.successful).getOrElse(addresses returning addresses.map(_.id) += lowerCaseAddress)
      } yield result match {
        case addressWithAntarcticGeographicPoint: Address if addressWithAntarcticGeographicPoint.geographicPoint == antarcticPoint &&
          lowerCaseAddress.geographicPoint != antarcticPoint =>
          val updatedAddress = lowerCaseAddress.copy(id = addressWithAntarcticGeographicPoint.id)
          update(updatedAddress) map {
            case int if int != 1 =>
              Logger.error("Address.save: not exactly one row was updated")
              addressWithAntarcticGeographicPoint
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
    case addressWitAntarcticGeographicPoint if addressWitAntarcticGeographicPoint.geographicPoint == antarcticPoint =>
      searchGeographicPoint.getGeographicPoint(addressWitAntarcticGeographicPoint, retry = 3) flatMap { save }
    case addressWithGeoPoint =>
      save(addressWithGeoPoint)
  }

  def saveEventRelation(eventAddressRelation: EventAddressRelation): Future[Int] =
    db.run(eventsAddresses += eventAddressRelation)

  def saveEventRelations(eventAddressRelations: Seq[EventAddressRelation]): Future[Boolean] =
    db.run(eventsAddresses ++= eventAddressRelations) map { _ =>
      true
    } recover {
      case e: Exception =>
        Logger.error("Address.saveEventRelations: ", e)
        false
    }

  def saveWithEventRelation(address: Address, eventId: Long): Future[Address] = save(address) flatMap { savedAddress =>
    saveEventRelation(EventAddressRelation(eventId, savedAddress.id.getOrElse(0))) map {
      case 1 =>
        savedAddress
      case _ =>
        Logger.error(s"Address.saveWithEventRelation: not exactly one row saved by Address.saveEventRelation for address $savedAddress and eventId $eventId")
        savedAddress
    }
  }
  
  def delete(id: Long): Future[Int] = db.run(addresses.filter(_.id === id).delete)
}