package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.Play.current
import services.Utilities.geographicPointToString


case class Address (addressId: Option[Long],
                    geographicPoint: Option[String],
                    city: Option[String],
                    zip: Option[String],
                    street: Option[String])

object Address {
  private val AddressParser: RowParser[Address] = {
    get[Long]("addressId") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[String]]("city") ~
      get[Option[String]]("zip") ~
      get[Option[String]]("street") map {
      case addressId ~ geographicPoint ~ city ~ zip ~ street =>
        Address(Option(addressId), geographicPoint, city, zip, street)
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

  def saveAddressAndEventRelation(address: Address, id: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      val addressId = SQL("""SELECT insertAddress({geographicPoint}, {city}, {zip}, {street})""")
        .on(
          'geographicPoint -> address.geographicPoint,
          'city -> address.city,
          'zip -> address.zip,
          'street -> address.street)
        .as(scalar[Long].single)
      saveEventAddressRelation(id, addressId)
    }
  } catch {
    case e: Exception => throw new DAOException("Address.saveAddressAndEventRelation: " + e.getMessage)
  }

  def saveEventAddressRelation(eventId: Long, addressId: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO eventsAddresses (eventId, addressId)
          | VALUES ({eventId}, {addressId})""".stripMargin)
        .on(
          'eventId -> eventId,
          'addressId -> addressId)
        .executeInsert()
    }
  } catch {
   case e: Exception => throw new DAOException("Address.saveEventAddressRelation: " + e.getMessage)
  }

  def deleteAddress(addressId: Long): Long = try {
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
}