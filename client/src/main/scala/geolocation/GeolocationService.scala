package geolocation

import com.greencatsoft.angularjs.core.{HttpService, Timeout}
import com.greencatsoft.angularjs.{Factory, Service, injectable}
import materialDesign.{MdToastService, MdToast}
import org.scalajs.dom.{console, navigator}
import org.scalajs.dom.raw.{Position, PositionError}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.UndefOr

case class GeographicPoint(lat: Double, lng: Double)

@injectable("geolocationService")
class GeolocationService(http: HttpService, timeout: Timeout) extends Service {

  val basePoint: GeographicPoint = GeographicPoint(45.7667, 4.8833)
  var geographicPoint: UndefOr[GeographicPoint] = js.undefined

  def getHtmlGeolocation(): Unit = {
    navigator.geolocation.getCurrentPosition((position: Position) => {
      geographicPoint = GeographicPoint(position.coords.latitude,position.coords.longitude)
    }, (error: PositionError) => {
      getIpGeolocation map { geographicPointIp =>
        geographicPoint = geographicPointIp
      }
    })
  }

  getHtmlGeolocation()

  def getIpGeolocation: Future[GeographicPoint] = {
    val getPoint = http.get[js.Any]("/users/geographicPoint")

    getPoint.error((error: Any) => geographicPoint = basePoint)

    getPoint map { response =>
      val responseMap = response.asInstanceOf[ArrayBuffer[Pair[String, Any]]].toMap
      if(responseMap.isDefinedAt("status")) {
        responseMap("status") match {
          case success if success == "success" =>
            GeographicPoint(responseMap("lat").toString.toDouble, responseMap("lng").toString.toDouble)
          case _ =>
            basePoint
        }
      } else {
        basePoint
      }
    }
  }

  val maxTry = 10
  var tryNumber = 0

  def getUserGeolocation: Future[GeographicPoint] = {
    timeout(() => console.log("retry get geoPoint"), 1000) flatMap { _ =>
      if (geographicPoint.isDefined) {
        Future(geographicPoint.get)
      } else {
        if (tryNumber < maxTry) {
          tryNumber += 1
          getUserGeolocation
        } else {
          geographicPoint = basePoint
          Future(geographicPoint.get)
        }
      }
    }
  }
}

@injectable("geolocationService")
class GeolocationServiceFactory(http: HttpService, timeout: Timeout) extends Factory[GeolocationService] {

  override def apply(): GeolocationService = new GeolocationService(http, timeout)
}
