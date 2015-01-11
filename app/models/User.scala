package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.{Json, JsNull, Writes}
import play.api.Play.current

case class User (id: Long,
                 email: String,
                 nickname : String,
                 password : String,
                 profile: String)

object User {
  /*implicit def pkWrites[T : Writes]: Writes[Pk[T]] = Writes {
    case anorm.Id(t) => implicitly[Writes[T]].writes(t)
    case anorm.NotAssigned => JsNull
  }*/
  implicit val userWrites = Json.writes[User]


  private val UserParser: RowParser[User] = {
    get[Long]("userId") ~
      get[String]("email") ~
      get[String]("nickname") ~
      get[String]("password") ~
      get[String]("profile")  map {
      case id ~ email ~ nickname ~ password ~ profile =>
        User(id, email, nickname, password, profile)
    }
  }

  def findAll(): Seq[User] = {
    DB.withConnection { implicit connection =>
      SQL("select * from users").as(UserParser *)
    }
  }

  def findAllByEvent(event: Event): Seq[User] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsUsers eU
             INNER JOIN users s ON s.userId = eU.userId where eU.eventId = {eventId}""")
        .on('eventId -> event.id)
        .as(UserParser *)
    }
  }

  def find(userId: Long): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from users WHERE userId = {userId}")
        .on('userId -> userId)
        .as(UserParser.singleOpt)
    }
  }

  def save(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
            INSERT INTO users(name)
            VALUES({name})
          """).on(
          'name -> name
        ).executeUpdate
    }

  }

}
