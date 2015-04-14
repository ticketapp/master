package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.Play.current
import controllers.DAOException
import services.Utilities.{geographicPointToString, getNormalizedWebsitesInText}

case class Place (placeId: Option[Long],
                  name: String,
                  facebookId: Option[String] = None,
                  geographicPoint: Option[String],
                  description: Option[String] = None,
                  webSites: Option[String] = None,
                  capacity: Option[Int] = None,
                  openingHours: Option[String] = None,
                  imagePath: Option[String] = None,
                  address : Option[Address] = None)

object Place {
  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r
  def formApply(name: String, facebookId: Option[String], geographicPoint: Option[String], description: Option[String],
                webSite: Option[String], capacity: Option[Int], openingHours: Option[String],
                imagePath: Option[String]): Place =
    new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath)

  def formUnapply(place: Place): Option[(String, Option[String], Option[String], Option[String], Option[String],
    Option[Int], Option[String], Option[String])] =
    Some((place.name, place.facebookId, place.geographicPoint, place.description, place.webSites, place.capacity,
      place.openingHours, place.imagePath))

  private val PlaceParser: RowParser[Place] = {
    get[Long]("placeId") ~
      get[String]("name") ~
      get[Option[String]]("facebookId") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[String]]("description") ~
      get[Option[String]]("webSites") ~
      get[Option[Int]]("capacity") ~
      get[Option[String]]("openingHours") ~
      get[Option[Long]]("addressId") ~
      get[Option[String]]("imagePath") map {
      case placeId ~ name ~ facebookId ~ geographicPoint ~ description ~ webSites ~ capacity ~ openingHours ~
        addressId  ~ imagePath =>
          Place(Option(placeId), name, facebookId, geographicPoint, description, webSites, capacity, openingHours, imagePath,
            Address.find(addressId))
    }
  }

  def save(place: Place): Option[Long] = try {
    DB.withConnection { implicit connection =>
      val addressId = place.address match {
        case None => None
        case Some(address: Address) => Some(address.addressId)
      }
      val organizerIdWithSameFacebookId = SQL(
        """SELECT placeId FROM places
          | WHERE facebookId = {facebookId}""".stripMargin)
        .on("facebookId" -> place.facebookId)
        .as(scalar[Long].singleOpt)
      SQL(
        s"""SELECT insertPlace({name}, {geographicPoint}, {addressId}, {facebookId}, {description},
           |{webSites}, {capacity}, {openingHours}, {imagePath}, {organizerId})""".stripMargin)
        .on(
          'name -> place.name,
          'addressId -> addressId,
          'facebookId -> place.facebookId,
          'geographicPoint -> place.geographicPoint,
          'description -> place.description,
          'webSites -> getNormalizedWebsitesInText(place.webSites).mkString(","),
          'capacity -> place.capacity,
          'openingHours -> place.openingHours,
          'imagePath -> place.imagePath,
          'organizerId -> organizerIdWithSameFacebookId)
        .as(scalar[Option[Long]].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.save: " + e.getMessage)
  }
  
  def find20Since(start: Int, center: String): Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT *
           |FROM places
           |ORDER BY geographicPoint <-> point '$center'
           |LIMIT 20 OFFSET $start""".stripMargin)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.find20Since: " + e.getMessage)
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

  def findAllAsTupleIdFacebookIdAndGeographicPoint: Seq[(Long, String, Option[String])] = try {
    val placeIdFacebookIdParser = {
      get[Long]("placeId") ~
        get[String]("facebookId") ~
        get[Option[String]]("geographicPoint") map {
        case placeId ~ facebookId ~ geographicPoint => (placeId, facebookId, geographicPoint)
      }
    }
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT placeId, facebookId, geographicPoint 
          |FROM places 
          |WHERE facebookId IS NOT NULL""".stripMargin)
        .as(placeIdFacebookIdParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAllIdsAndFacebookIds: " + e.getMessage)
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

  def followPlace(placeId : Long)={//: Option[Long] = {
    /*try {
      DB.withConnection { implicit connection =>
        SQL("insert into placesFollowed(userId, placeId) values ({userId}, {placeId})").on(
          'userId -> userId,
          'placeId -> placeId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow place: " + e.getMessage)
    }*/
  }

  def saveEventPlaceRelation(eventId: Long, placeId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT insertEventPlaceRelation({eventId}, {placeId})""")
        .on(
          'eventId -> eventId,
          'placeId -> placeId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Place.saveEventPlaceRelation: " + e.getMessage)
  }
}
