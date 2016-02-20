package events

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{AbstractController, injectable}
import geolocation.GeolocationService
import httpServiceFactory.HttpGeneralService
import upickle.default._
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("eventsController")
class EventsController(eventScope: EventsScope, service: HttpGeneralService, timeout: Timeout,
                       geolocationService: GeolocationService, eventsService: EventsService)
    extends AbstractController[EventsScope](eventScope) with jsonHelper {

  def findMaybeSalableEventsContaining(pattern: String): Unit = {
    service.get(EventsRoutes.findMaybeSalableEvents(pattern)) map { foundEvents =>
      timeout(() => eventScope.maybeSalableEvents = read[Seq[MaybeSalableEvent]](foundEvents).toJSArray)
    }
  }

  def findById(id: Int): Unit = eventsService.findById(id) map { event =>
    timeout(() => eventScope.events = js.Array(event))
  }

  def findNearActualPosition(offset: Int, numberToReturn: Int): Unit = {
    geolocationService.getUserGeolocation map { geographicPoint =>
      val geoPoint = geographicPoint.lat + "," + geographicPoint.lng
      service.get(EventsRoutes.find(offset, numberToReturn, geoPoint)) map { foundEvents =>
        timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
      }
    }
  }

  def find(offset: Int, numberToReturn: Int, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.find(offset, numberToReturn, geographicPoint)) map { foundEvents =>
      timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findInHourInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.FindInHourInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findPassedInInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.FindPassedInInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findAllContaining(pattern: String, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.findAllContaining(pattern, geographicPoint)) map { foundEvents =>
      timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findByCityPattern(pattern: String): Unit = {
    service.get(EventsRoutes.findByCityPattern(pattern)) map { foundEvents =>
      timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(EventsRoutes.findNearCity(city, numberToReturn, offset)) map { foundEvents =>
      timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def createByFacebookId(facebookId: String): Unit = {
    service.post(EventsRoutes.createByFacebookId(facebookId)) map { foundEvents =>
      timeout(() => eventScope.events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }
}
