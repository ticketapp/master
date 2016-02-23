package events

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{AbstractController, injectable}
import geolocation.GeolocationService
import httpServiceFactory.HttpGeneralService
import materialDesign.MdToastService
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("eventsController")
class EventsController(eventScope: EventsScope, service: HttpGeneralService, timeout: Timeout,
                       geolocationService: GeolocationService, mdToast: MdToastService, eventsService: EventsService)
      extends AbstractController[EventsScope](eventScope) with jsonHelper {

  var initLat =  0.0
  val coordinateMaxLength = 8
  var initLng = 0.0

  geolocationService.getUserGeolocation map { geolocation =>
    timeout(() => initLat = geolocation.lat.toString.substring(0, coordinateMaxLength).toDouble)
  }

  geolocationService.getUserGeolocation map { geolocation =>
    timeout(() => initLng = geolocation.lng.toString.substring(0, coordinateMaxLength).toDouble)
  }

  def update(event: js.Any): Unit = {
    service.updateWithObject(EventsRoutes.update(), event) map { response =>
      val toast = mdToast.simple("event update ok")
      mdToast.show(toast)
    }
  }

  def findMaybeSalableEventsContaining(pattern: String): Unit = {
    service.get(EventsRoutes.findMaybeSalableEvents(pattern)) map { foundEvents =>
      timeout(() => eventScope.maybeSalableEvents = JSON.parse(foundEvents))
    }
  }

  def findById(id: Int): Unit =
    eventsService.findByIdAsJson(id) map(event => timeout(() => eventScope.events = js.Array(event)))

  def findNearActualPosition(offset: Int, numberToReturn: Int): Unit = {
    geolocationService.getUserGeolocation map { geographicPoint =>
      val geoPoint = geographicPoint.lat + "," + geographicPoint.lng
      service.get(EventsRoutes.find(offset, numberToReturn, geoPoint)) map { foundEvents =>
        timeout(() => eventScope.events = JSON.parse(foundEvents))
      }
    }
  }

  def find(offset: Int, numberToReturn: Int, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.find(offset, numberToReturn, geographicPoint)) map { foundEvents =>
      timeout(() => eventScope.events = JSON.parse(foundEvents))
    }
  }

  def findInHourInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.findInHourInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout(() => eventScope.events = JSON.parse(foundEvents))
    }
  }

  def findPassedInInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.findPassedInInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout(() => eventScope.events = JSON.parse(foundEvents))
    }
  }

  def findAllContaining(pattern: String, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.findAllContaining(pattern, geographicPoint)) map { foundEvents =>
      timeout(() => eventScope.events = JSON.parse(foundEvents))
    }
  }

  def findByCityPattern(pattern: String): Unit = {
    service.get(EventsRoutes.findByCityPattern(pattern)) map { foundEvents =>
      timeout(() => eventScope.events = JSON.parse(foundEvents))
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(EventsRoutes.findNearCity(city, numberToReturn, offset)) map { foundEvents =>
      timeout(() => eventScope.events = JSON.parse(foundEvents))
    }
  }

  def createByFacebookId(facebookId: String): Unit = {
    service.post(EventsRoutes.createByFacebookId(facebookId)) map { foundEvents =>
      timeout(() => eventScope.events = JSON.parse(foundEvents))
    }
  }
}
