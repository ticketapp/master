package geolocation

import com.greencatsoft.angularjs.core.{HttpService, Timeout}
import com.greencatsoft.angularjs.{Factory, Service, injectable}
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

  def getIpGeolocation: Future[GeographicPoint] = {
    val getPoint = http.get[js.Any]("/users/geographicPoint")
    getPoint.error((error: Any) => geographicPoint = GeographicPoint(45.7667, 4.8833))
    getPoint map { response =>
      val responseMap = response.asInstanceOf[ArrayBuffer[Pair[String, Any]]].toMap
      if(responseMap.isDefinedAt("status")) {
        responseMap("status") match {
          case success if success == "success" =>
            GeographicPoint(responseMap("lat").toString.toDouble, responseMap("lng").toString.toDouble)
          case _ =>
            GeographicPoint(45.7667, 4.8833)
        }
      } else {
        GeographicPoint(45.7667, 4.8833)
      }
    }
  }

  def getUserGeolocation: Future[GeographicPoint] = {
    timeout(() => getHtmlGeolocation(), 300) flatMap { htmlGeolocation =>
      if(geographicPoint.isDefined) {
        Future(geographicPoint.get)
      }
      else {
        getHtmlGeolocation()
        getUserGeolocation
      }
    }
  }
}

@injectable("geolocationService")
class GeolocationServiceFactory(http: HttpService, timeout: Timeout) extends Factory[GeolocationService] {

  override def apply(): GeolocationService = new GeolocationService(http, timeout)
}
