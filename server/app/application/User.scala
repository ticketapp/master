package application

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import database.{MyPostgresDriver, MyDBTableDefinitions}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.postfixOps


case class User(uuid: UUID,
                loginInfo: LoginInfo,
                firstName: Option[String],
                lastName: Option[String],
                fullName: Option[String],
                email: Option[String],
                avatarURL: Option[String]) extends Identity

case class GuestUser(ip: String, userUuid: Option[UUID])

class UserMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  implicit val userWrites = Json.writes[User]

  def findGuestUserByIp(ip: String): Future[Option[GuestUser]] = db.run(guestUsers.filter(_.ip === ip).result.headOption)

  def saveGuestUser(guestUser: GuestUser): Future[Int] = db.run(guestUsers += guestUser)

  def findUUIDOfTracksRemoved(userUUID: UUID): Future[Seq[UUID]] = db.run((for {
    trackRating <- trackRatings if trackRating.userId === userUUID && trackRating.reason.nonEmpty
  } yield trackRating.trackId).result)
}