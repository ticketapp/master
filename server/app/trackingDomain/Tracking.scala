package trackingDomain

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

import application.GuestUser
import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class UserSession(id: UUID, ip: String)
case class UserAction(action: String, timestamp: Timestamp, sessionId: UUID)

class TrackingMethods @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def saveUserSession(userSession: UserSession): Future[Int] = {
    val guestUser = db.run(
      guestUsers.filter(_.ip === userSession.ip).exists.result
    )
    def bQuery(exist: Boolean) =
      if (exist) db.run(userSessions += userSession)
      else {
        val saveGuest = db.run(guestUsers += GuestUser(userSession.ip, None))
        saveGuest flatMap { savedGuestResponse =>
          db.run(userSessions += userSession)
        }
      }
    guestUser flatMap { exist =>
      bQuery(exist)
    }
  }

  def findUserSessions: Future[Seq[UserSession]] = db.run(userSessions.result)

  def saveUserAction(userAction: UserAction): Future[Int] = db.run(userActions += userAction)

  def findUserActionBySessionId(sessionId: UUID): Future[Seq[UserAction]] =
    db.run(userActions.filter(_.sessionId === sessionId).result)

}

