package models

import anorm.SqlParser._
import anorm._
import controllers.{UserOAuth2InfoWronglyFormatted, DAOException}
import play.api.Logger
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

  def findAll(): Seq[User] = try {
    DB.withConnection { implicit connection =>
      SQL("select * from users_login").as(UserParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("User.findAll: " + e.getMessage)
  }

  def findAllByEvent(event: Event): Seq[User] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsUsers eU
             INNER JOIN users s ON s.userId = eU.userId where eU.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(UserParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("User.findAllByEvent: " + e.getMessage)
  }

  def findAllContaining(pattern: String): Seq[User] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM users_login
          | WHERE LOWER(nickname)
          |   LIKE '%'||{patternLowCase}||'%'
          | LIMIT 3""".stripMargin)
        .on('patternLowCase -> pattern.toLowerCase)
        .as(UserParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("User.findAllContaining: " + e.getMessage)
  }

  def find(userId: Long): Option[User] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from users_login WHERE userId = {userId}")
        .on('userId -> userId)
        .as(UserParser.singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("User.find: " + e.getMessage)
  }

  def formApply(email: String, nickname: String, password: String, profile: String): User =
    new User(-1L, new Date, email, nickname, password, profile)

  def formUnapply(user: User): Option[(String, String, String, String)] =
    Some((user.email, user.nickname, user.password, user.profile))

  def save: Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO users_login(userId, providerId, firstName, lastName, fullName, authMethod)
          | VALUES ('userId', 'providerId', 'firstName', 'lastName', 'fullName', 'oauth2')""".stripMargin)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("User.save: " + e.getMessage)
  }

  def delete(userId: String): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users_login WHERE userId = {userId}")
        .on('userId -> userId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("User.delete: " + e.getMessage)
  }

  def findFacebookAccessToken(userId: String): Option[String] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT oAuth2Info FROM users_login
          | WHERE userId = {userId} """.stripMargin)
        .on('userId -> userId )
        .as(scalar[String].singleOpt) match {
        case None => None
        case Some(oAuth2info) =>
          try {
            Option(oAuth2info.split("\"")(3))
          } catch {
            case e:Exception => throw new UserOAuth2InfoWronglyFormatted("User.findFacebookAccessToken")
          }
      }
    }
  } catch {
    case e: Exception => throw new DAOException("User.findFacebookAccessToken: " + e.getMessage)
  }

  def getTracksRemoved(userId: String): Seq[Track] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM tracksRating tracksRating
          |  INNER JOIN tracks tracks
          |    ON tracks.trackId = tracksRating.trackId
          |    WHERE tracksRating.userId = {userId} AND tracksRating.reason IS NOT NULL""".stripMargin)
        .on('userId -> userId)
        .as(Track.trackParser *)
    }
  } catch {
    case e: Exception =>
      Logger.error("User.getTracksRemoved: ", e)
      Seq.empty
  }
}
