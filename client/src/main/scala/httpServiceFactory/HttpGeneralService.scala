package httpServiceFactory

import AdminClient.A
import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.{Factory, Service, injectable}
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.{Failure, Success, Try}
import upickle.default._
import scala.concurrent.ExecutionContext.Implicits.global

@injectable("adminService")
class HttpGeneralService(http: HttpService) extends Service {
  require(http != null, "Missing argument 'http'.")


  def getJsonAndRead(url: String): Future[Seq[A]] = {
    val getFuture = http.get[js.Any](url) // implicit conversion occurs here.
    getFuture.onFailure {
      case err =>
    }
    val intermediateFuture: Future[Seq[A]] = getFuture.map(JSON.stringify(_)).map(read[Seq[A]])
    intermediateFuture
  }

}
@injectable("adminServiceFactory")
class HttpServiceFactory(http: HttpService) extends Factory[HttpGeneralService] {

  override def apply(): HttpGeneralService = new HttpGeneralService(http)
}
