package userDomain

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Authorization, Identity, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import database.{MyDBTableDefinitions, MyPostgresDriver}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Request
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

case class Rib(id: Option[Long],
               bankCode: String,
               deskCode: String,
               accountNumber: String,
               ribKey: String,
               userId: UUID)

case class FromClientRib(id: Option[Long],
                         bankCode: String,
                         deskCode: String,
                         accountNumber: String,
                         ribKey: String)

case class IdCard(uuid: UUID, userId: UUID)

case class Administrator() extends Authorization[User, CookieAuthenticator] {
  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(
    implicit request: Request[B], messages: Messages) = {
    val administratorId = "560731184063043"
    Future.successful(user.loginInfo.providerKey == administratorId)
  }
}

class UserMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  implicit val userWrites = Json.writes[User]
  
  def fromClientRibToRib(fromClientRib: FromClientRib, userUUID: UUID): Rib = {
    Rib(
      id = fromClientRib.id,
      bankCode = fromClientRib.bankCode,
      deskCode = fromClientRib.deskCode,
      accountNumber = fromClientRib.accountNumber,
      ribKey = fromClientRib.ribKey,
      userId = userUUID
    )
  }

  def findGuestUserByIp(ip: String): Future[Option[GuestUser]] = db.run(guestUsers.filter(_.ip === ip).result.headOption)

  def saveGuestUser(guestUser: GuestUser): Future[Int] = db.run(guestUsers += guestUser)

  def findUUIDOfTracksRemoved(userUUID: UUID): Future[Seq[UUID]] = db.run((for {
    trackRating <- trackRatings if trackRating.userId === userUUID && trackRating.reason.nonEmpty
  } yield trackRating.trackId).result)

  def createRib(rib: Rib): Future[Int] = db.run(ribs += rib)

  def updateRib(rib: Rib): Future[Int] =
    db.run(ribs.filter(foundRib => foundRib.id === rib.id && foundRib.userId === rib.userId).update(rib))

  def findRibsByUserId(userId: UUID): Future[Seq[Rib]] = db.run(ribs.filter(_.userId === userId).result)

  def createIdCard(idCard: IdCard): Future[Int] = db.run(idCards += idCard)

  def findIdCardsByUserId(userId: UUID): Future[Seq[IdCard]] = db.run(idCards.filter(_.userId === userId).result)
}
