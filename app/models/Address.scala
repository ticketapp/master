package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.Play.current
import services.Utilities.geographicPointToString


case class Address (addressId: Long,
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
        Address(addressId, geographicPoint, city, zip, street)
    }
  }

  def formApply(city: Option[String], zip: Option[String], street: Option[String]) =
    new Address(-1L, None, city, zip, street)
  def formUnapply(address: Address): Option[(Option[String], Option[String], Option[String])] =
    Some((address.city, address.zip, address.street))


  def findAll: List[Address] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM addresses").as(AddressParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Address.findAll: " + e.getMessage)
  }

  def findAllByEvent(event: Event): List[Address] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsAddresses eA
             INNER JOIN addresses a ON a.addressId = eA.addressId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(AddressParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Address.findAllByEvent: " + e.getMessage)
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
      case e: Exception => throw new DAOException("Problem with the method Address.find: " + e.getMessage)
    }
  }
  /*
  def findByOrganizer(organizerId: Long): Option[Address] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM addresses WHERE addressId = {addressId}")
        .on('organizerId -> organizerId)
        .as(AddressParser.singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Address.find: " + e.getMessage)
  }
*/
  def findAllContaining(pattern: String): Seq[Address] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM addresses WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
        .on('patternLowCase -> pattern.toLowerCase)
        .as(AddressParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Address.findAllContaining: " + e.getMessage)
  }


  def saveAddressAndEventRelation(address: Address, id: Long): Option[Long] = {
    //, relationClass: String
    //sécurité geographicPoint
    address.geographicPoint match {
      case None =>
        try {
          DB.withConnection { implicit connection =>
            SQL( s"""INSERT INTO addresses(city, zip, street)
              VALUES ({city}, {zip}, {street})""")
              .on(
                'city -> address.city,
                'zip -> address.zip,
                'street -> address.street
              ).executeInsert() match {
                case Some(x: Long) => saveEventAddressRelation(id, x)
                case _ => None
              }
          }
        } catch {
          case e: Exception => throw new DAOException("Cannot create address: " + e.getMessage)
        }

      case Some(geoPoint: String) => try {
        val geoPointValue = geoPoint
        DB.withConnection { implicit connection =>
          SQL( s"""INSERT INTO addresses(geographicPoint, city, zip, street)
            VALUES (point '$geoPointValue', {city}, {zip}, {street})""")
            .on(
              'city -> address.city,
              'zip -> address.zip,
              'street -> address.street)
            .executeInsert() match {
              case Some(x: Long) => saveEventAddressRelation(id, x)
              case _ => None
            }
        }
      } catch {
        case e: Exception => throw new DAOException("Cannot create address: " + e.getMessage)
      }
    }
  }

   def saveEventAddressRelation(eventId: Long, addressId: Long): Option[Long] = try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO eventsAddresses (eventId, addressId)
          VALUES ({eventId}, {addressId})""")
          .on(
            'eventId -> eventId,
            'addressId -> addressId)
          .executeInsert()
      }
   } catch {
     case e: Exception => throw new DAOException("saveEventAddressRelation: " + e.getMessage)
   }

  def deleteAddress(addressId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM addresses WHERE addressId={addressId}""")
        .on('addressId -> addressId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot delete address: " + e.getMessage)
  }

  def followAddress(userId : Long, addressId : Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("insert into addressFollowed(userId, addressId) values ({userId}, {addressId})").on(
        'userId -> userId,
        'addressId -> addressId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot follow address: " + e.getMessage)
  }
}