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
    var message = ""
    status match {
      case 401 =>
        message = "Unauthorized"
      case 404 =>
        message = "Not found"
    }
    val toast = mdToast.simple(message)
    mdToast.show(toast)
    console.error(s"An error has occured: $error")
  }

  @JSExport
  def get(url: String): Future[String] = {
    val getFuture = http.get[js.Any](url) // implicit conversion occurs here.
    getFuture.error(errors)
    getFuture.map { a =>
      JSON.stringify(a)
    }
  }

  def post(url: String): Future[String] = {
    val postFuture = http.post[js.Any](url) // implicit conversion occurs here.
    postFuture.error(errors)
    val intermediateFuture: Future[String] = postFuture.map(JSON.stringify(_))
    intermediateFuture
  }

  def postWithObject(url: String, objectToPost: js.Any): Future[String] = {
    val postFuture = http.post[js.Any](url, objectToPost) // implicit conversion occurs here.
    postFuture.error(errors)
    val intermediateFuture: Future[String] = postFuture.map(JSON.stringify(_))
    intermediateFuture
  }

}
@injectable("httpGeneralService")
class HttpGeneralServiceFactory(http: HttpService, mdToast: MdToastService) extends Factory[HttpGeneralService] {

  override def apply(): HttpGeneralService = new HttpGeneralService(http, mdToast)
}
