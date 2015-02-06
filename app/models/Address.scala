package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Address (addressId: Long,
                    isEvent: Boolean,
                    isPlace: Boolean,
                    geographicPoint: Option[String],
                    city: Option[String],
                    zip: Option[String],
                    street: Option[String])

object Address {
  implicit val addressWrites = Json.writes[Address]

  private val AddressParser: RowParser[Address] = {
    get[Long]("addressId") ~
      get[Boolean]("isEvent") ~
      get[Boolean]("isPlace") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[String]]("city") ~
      get[Option[String]]("zip") ~
      get[Option[String]]("street") map {
      case addressId ~ isEvent ~ isPlace ~ geographicPoint ~ city ~ zip ~ street =>
        Address(addressId, isEvent, isPlace, None, city, zip, street)
    }
  }

  def findAll(): Seq[Address] = {
    DB.withConnection { implicit connection =>
      SQL("select * from addresss").as(AddressParser *)
    }
  }

  def findAllByEvent(event: Event): Seq[Address] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsAddresss eA
             INNER JOIN addresss a ON a.addressId = eA.addressId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(AddressParser *)
    }
  }

  def find(addressId: Long): Option[Address] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from addresss WHERE addressId = {addressId}")
        .on('addressId -> addressId)
        .as(AddressParser.singleOpt)
    }
  }

  def findAllContaining(pattern: String): Seq[Address] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM addresss WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(AddressParser *)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Address.findAllContaining: " + e.getMessage)
    }
  }

  //def formApply(facebookId: Option[String], name: String): Address = new Address(-1L, new Date, facebookId, name, None)
  //def formUnapply(address: Address): Option[(Option[String], String)] = Some((address.facebookId, address.name))


  def saveAddressAndEventRelation(address: Address, id: Long): Option[Long] = {
    //, relationClass: String
    //sécurité geographicPoint

    address.geographicPoint match {
      case None =>
        try {
          DB.withConnection { implicit connection =>
            SQL( s"""INSERT into addresses(isEvent, isPlace, city, zip, street)
          values ({isEvent}, {isPlace}, {city}, {zip}, {street})"""
            ).on(
                'isEvent -> address.isEvent,
                'isPlace -> address.isPlace,
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
          SQL( s"""INSERT into addresses(isEvent, isPlace, geographicPoint, city, zip, street)
          values ({isEvent}, {isPlace}, point '$geoPointValue', {city}, {zip}, {street})"""
          ).on(
              'isEvent -> address.isEvent,
              'isPlace -> address.isPlace,
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
    }
  }

   def saveEventAddressRelation(eventId: Long, addressId: Long): Option[Long] = {
      try {
        DB.withConnection { implicit connection =>
          SQL( """INSERT INTO eventsAddresses (eventId, addressId)
            VALUES ({eventId}, {addressId})""").on(
              'eventId -> eventId,
              'addressId -> addressId
            ).executeInsert()
        }
      } catch {
        case e: Exception => throw new DAOException("saveEventAddressRelation: " + e.getMessage)
      }
    }


  def deleteAddress(addressId: Long): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("DELETE FROM address WHERE addressId={addressId}"
        ).on(
          'addressId -> addressId
        ).executeUpdate()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot delete address: " + e.getMessage)
    }
  }

  def followAddress(userId : Long, addressId : Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into addressFollowed(userId, addressId) values ({userId}, {addressId})").on(
          'userId -> userId,
          'addressId -> addressId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow address: " + e.getMessage)
    }
  }
}