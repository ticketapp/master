package httpServiceFactory

import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.{Factory, Service, injectable}
import materialDesign.MdToastService
import org.scalajs.dom.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExport

@injectable("httpGeneralService")
class HttpGeneralService(http: HttpService, mdToast: MdToastService) extends Service {
  require(http != null, "Missing argument 'http'.")

  val errors = (error: Any, status: Int) => {
    val message = status match {
      case 401 => "Unauthorized"
      case 404 => "Not found"
    }

    val toast = mdToast.simple(message)
    mdToast.show(toast)
    console.error(s"An error has occurred: $error")
  }

  @JSExport
  def get(url: String): Future[String] = {
    val getFuture = http.get[js.Any](url)
    getFuture.error(errors)
    getFuture.map { response => JSON.stringify(response) }
  }

  def post(url: String): Future[String] = {
    val postFuture = http.post[js.Any](url)
    postFuture.error(errors)
    val intermediateFuture: Future[String] = postFuture.map(JSON.stringify(_))
    intermediateFuture
  }

  def postWithObject(url: String, objectToPost: js.Any): Future[String] = {
    val postFuture = http.post[js.Any](url, objectToPost)
    postFuture.error(errors)
    val intermediateFuture: Future[String] = postFuture.map(JSON.stringify(_))
    intermediateFuture
  }

  def updateWithObject(url: String, objectToPost: js.Any): Future[String] = {
    val postFuture = http.put[js.Any](url, objectToPost)
    postFuture.error(errors)
    postFuture.map(JSON.stringify(_))
  }
}

@injectable("httpGeneralService")
class HttpGeneralServiceFactory(http: HttpService, mdToast: MdToastService) extends Factory[HttpGeneralService] {

  override def apply(): HttpGeneralService = new HttpGeneralService(http, mdToast)
}
