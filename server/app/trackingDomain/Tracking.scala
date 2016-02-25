package trackingDomain

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject
import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import userDomain.GuestUser

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class UserSession(uuid: UUID, ip: String, screenWidth: Int, screenHeight: Int)
case class UserAction(action: String, timestamp: Timestamp, sessionId: UUID)

class TrackingMethods @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def saveUserSession(userSession: UserSession): Future[Int] = {
    val guestUser = db.run(guestUsers.filter(_.ip === userSession.ip).exists.result)
    
    def doSave(exist: Boolean) =
      if(exist)
        db.run(userSessions += userSession)
      else {
        val saveGuest = db.run(guestUsers += GuestUser(userSession.ip, None))
        saveGuest flatMap { savedGuestResponse =>
          db.run(userSessions += userSession)
        }
      }
    
    guestUser flatMap { exist => doSave(exist) }
  }

  def findUserSessions: Future[Seq[UserSession]] = db.run(userSessions.result)

  def findInProgressSession: Future[Seq[UserSession]] = {
    /*val now = DateTime.now()
    val fiveMinutesAgo = now.minusMinutes(5).getMillis
    val query =
      for {
        session <- userSessions joinLeft userActions on (_.sessionUuid === _.sessionId)
      } yield session

    db.run(query.result) map { sessionsWithMaybeActions =>
      val groupedSessions = sessionsWithMaybeActions.groupBy(_._1) map { sessionWithMaybeActions =>
        val sessionWithCollectedActions= (sessionWithMaybeActions._1, sessionWithMaybeActions._2.collect {
          case (_, Some(action)) =>
            action
        })
        sessionWithCollectedActions
      }
      groupedSessions.toSeq.collect {
        case (session, actions) if actions.exists(_.timestamp.getTime > fiveMinutesAgo) =>
          session
      }
    }*/
    Future(Seq.empty)
  }

  def saveUserAction(userAction: UserAction): Future[Int] = db.run(userActions += userAction)

  def findUserActionBySessionId(sessionId: UUID): Future[Seq[UserAction]] =
    db.run(userActions.filter(_.sessionId === sessionId).result)
}

