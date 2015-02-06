package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.{Json, JsNull, Writes}
import play.api.Play.current
import java.util.Date

case class User (userId: Long,
                 creationDateTime: Date,
                 email: String,
                 nickname : String,
                 password : String,
                 profile: String)

object User {
  implicit val userWrites = Json.writes[User]

  private val UserParser: RowParser[User] = {
    get[Long]("userId") ~
      get[Date]("creationDateTime") ~
      get[String]("email") ~
      get[String]("nickname") ~
      get[String]("password") ~
      get[String]("profile")  map {
      case id ~ creationDateTime ~ email ~ nickname ~ password ~ profile =>
        User(id, creationDateTime, email, nickname, password, profile)
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
        .on('eventId -> event.eventId)
        .as(UserParser *)
    }
  }

  def findAllContaining(pattern: String): Seq[User] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM users WHERE LOWER(nickname) LIKE '%'||{patternLowCase}||'%' LIMIT 3")
          .on('patternLowCase -> pattern.toLowerCase())
          .as(UserParser *)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method User.findAllContaining: " + e.getMessage)
    }
  }

  def find(userId: Long): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from users WHERE userId = {userId}")
        .on('userId -> userId)
        .as(UserParser.singleOpt)
    }
  }

  def formApply(email: String, nickname: String, password: String, profile: String): User =
    new User(-1L, new Date, email, nickname, password, profile)

  def formUnapply(user: User): Option[(String, String, String, String)] =
    Some((user.email, user.nickname, user.password, user.profile))


  def saveUser(user: User) = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """INSERT INTO users(email, nickname, password, profile)
              VALUES({email}, {nickname}, {password}, {profile})"""
        ).on(
            'email -> user.email,
            'nickname -> user.nickname,
            'password -> user.password,
            'profile -> user.profile
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save user: " + e.getMessage)
    }
  }
}
