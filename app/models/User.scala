package models

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
import services.MyPostgresDriver
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

class UserMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {


  implicit val userWrites = Json.writes[User]

  def findUUIDOfTracksRemoved(userUUID: UUID): Future[Seq[UUID]] = db.run((for {
    trackRating <- trackRatings if trackRating.userId === userUUID && trackRating.reason.nonEmpty
  } yield trackRating.trackId).result)
}
