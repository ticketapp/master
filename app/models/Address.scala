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

  implicit def geographicPointToString: Column[String] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case d: Any => Right(d.toString)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass +
        " to Float for column " + qualified) )
    }
  }

  private val AddressParser: RowParser[Address] = {
    get[Long]("addressId") ~
      get[Boolean]("isEvent") ~
      get[Boolean]("isPlace") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[String]]("city") ~
      get[Option[String]]("zip") ~
      get[Option[String]]("street") map {
      case addressId ~ isEvent ~ isPlace ~ geographicPoint ~ city ~ zip ~ street =>
        Address(addressId, isEvent, isPlace, geographicPoint, city, zip, street)
    }
  }

  def formApply(city: String, zip: String, street: String) = {
    new Address(-1L, true, false, None, Some(city), Some(zip), Some(street))
  }
  def formUnapply(address: Address): Option[(String, String, String)] =
    Some((address.city.get, address.zip.get, address.street.get))
  //def formUnapply(artist: Artist): Option[(Option[String], String)] = Some((artist.facebookId, artist.name))

  def findAll(): List[Address] = {
    DB.withConnection { implicit connection =>
      SQL("select * from addresses").as(AddressParser.*)
    }
  }

  def findAllByEvent(event: Event): List[Address] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsAddresses eA
             INNER JOIN addresses a ON a.addressId = eA.addressId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(AddressParser.*)
    }
  }

  def find(addressId: Option[Long]): Option[Address] = {
    addressId match {
      case None => None
      case Some(addressId) => try {
        DB.withConnection { implicit connection =>
          SQL("SELECT * from addresses WHERE addressId = {addressId}")
            .on('addressId -> addressId)
            .as(AddressParser.singleOpt)
        }
      } catch {
        case e: Exception => throw new DAOException("Problem with the method Address.find: " + e.getMessage)
      }
    }
  }

  def findAllContaining(pattern: String): Seq[Address] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM addresses WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(AddressParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Address.findAllContaining: " + e.getMessage)
    }
  }

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
        SQL("""DELETE FROM addresses WHERE addressId={addressId}""").on('addressId -> addressId).executeUpdate()
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