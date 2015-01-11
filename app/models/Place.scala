package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import controllers.DAOException

/**
 * Created by sim on 03/10/14.
 */
case class Place (placeId: Long,
                  name: String,
                  eventId: Option[Long] = None,
                  addressID: Option[Long] = None)

object Place {
  implicit val placeWrites = Json.writes[Place]


  private val PlaceParser: RowParser[Place] = {
    get[Long]("placeId") ~
      get[String]("name") ~
      get[Option[Long]]("eventId") ~
      get[Option[Long]]("addressID") map {
        case placeId ~ name ~ eventId ~ addressID=>
          Place(placeId, name, eventId, addressID)
    }
  }

  def findAll(): Seq[Place] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places").as(PlaceParser *)
    }
  }

  def findAllByEvent(event: Event): Seq[Place] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsPlaces eP
             INNER JOIN places s ON s.placeId = eP.placeId where eP.eventId = {eventId}""")
        .on('eventId -> event.id)
        .as(PlaceParser *)
    }
  }

  def find(placeId: Long): Option[Place] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from places WHERE placeId = {placeId}")
        .on('placeId -> placeId)
        .as(PlaceParser.singleOpt)
    }
  }

  def followPlace(userId : Long, placeId : Long): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into placesFollowed(userId, placeId) values ({userId}, {placeId})").on(
          'userId -> userId,
          'placeId -> placeId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow place: " + e.getMessage)
    }
  }
}
