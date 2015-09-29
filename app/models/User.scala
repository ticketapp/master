package models

import java.sql.Timestamp
import java.util.{Date, UUID}

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api.Identity
import controllers.{DAOException, UserOAuth2InfoWronglyFormatted}
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current

import play.api.libs.json.Json
import slick.driver.PostgresDriver.api._
import slick.model.ForeignKeyAction
import scala.language.postfixOps
/*import com.github.tototoshi.slick.PostgresJodaSupport._
import driver.api._*/


case class User (UUID: UUID,
                 creationDateTime: DateTime,
                 email: String,
                 nickname : String,
                 password : String,
                 profile: String) extends Identity

object User {
  implicit def dateTime =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts.getTime)
    )

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def UUID = column[UUID]("userId", O.PrimaryKey, O.AutoInc)
    def creationDateTime = column[DateTime]("creationDateTime")
    def email = column[String]("email")
    def nickname = column[String]("nickname")
    def password = column[String]("password")
    def profile = column[String]("profile")
    def * = (UUID, creationDateTime, email, nickname, password, profile) <> ((User.apply _).tupled, User.unapply)
  }

  val users = TableQuery[Users]

  implicit val userWrites = Json.writes[User]

  private val UserParser: RowParser[User] = {
    get[UUID]("userId") ~
      get[DateTime]("creationDateTime") ~
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
    User(UUID.randomUUID(), new DateTime, email, nickname, password, profile)

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

  def findFacebookAccessToken(userUUID: UUID): Option[String] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT oAuth2Info FROM users_login
          | WHERE userId = {userId} """.stripMargin)
        .on('userId -> userUUID )
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

  def getTracksRemoved(userUUID: UUID): Seq[Track] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM tracksRating tracksRating
          |  INNER JOIN tracks tracks
          |    ON tracks.trackId = tracksRating.trackId
          |    WHERE tracksRating.userId = {userId} AND tracksRating.reason IS NOT NULL""".stripMargin)
        .on('userId -> userUUID)
        .as(Track.trackParser *)
    }
  } catch {
    case e: Exception =>
      Logger.error("User.getTracksRemoved: ", e)
      Seq.empty
  }
}
