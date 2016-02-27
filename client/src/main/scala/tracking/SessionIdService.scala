package tracking

import com.greencatsoft.angularjs.{Factory, injectable, Service}
import com.greencatsoft.angularjs.core.Timeout
import httpServiceFactory.HttpGeneralService
import org.scalajs.dom._
import upickle.default._
import utilities.NgCookies
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

@injectable("sessionIdService")
class SessionIdService(timeout: Timeout, ngCookies: NgCookies, httpService: HttpGeneralService) extends Service {

  def aaa: Int = 5

  def getMaybeSessionId: Option[String] = ngCookies.get("sessionId") match {
    case string if string.isInstanceOf[String] => Option(string.asInstanceOf[String])
    case _ => None
  }

  def setSessionId(sessionId: String): Unit = ngCookies.put("sessionId", sessionId)

  def postNewSession(): Future[String] = {
    httpService.post(TrackingRoutes.postSession(
      screenWidth = window.innerWidth,
      screenHeight = window.innerHeight)
    ) map read[String]
  }

  def getOrSetSessionId(): Future[String] = getMaybeSessionId match {
    case None => createNewSession()

    case Some(alreadyExistingSessionId) => Future(alreadyExistingSessionId)
  }

  def createNewSession(): Future[String] = {
    postNewSession map { newSessionId =>
      setSessionId(newSessionId)
      newSessionId
    }
  }
}

@injectable("sessionIdService")
class SessionIdServiceFactory(timeout: Timeout, ngCookies: NgCookies, httpService: HttpGeneralService)
    extends Factory[SessionIdService] {
  override def apply() = new SessionIdService(timeout: Timeout, ngCookies: NgCookies, httpService: HttpGeneralService)
}
