package events

import com.greencatsoft.angularjs.core.{Timeout, Scope}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import geolocation.GeolocationService
import httpServiceFactory.HttpGeneralService
import utilities.jsonHelper

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.{UndefOr, Date, JSON}
import scala.scalajs.js.annotation.{JSExport, JSExportAll}
import org.scalajs.dom.console
import upickle.Js
import upickle.default._
@JSExportAll
@injectable("eventsController")
class EventsController(eventScope: EventsScopeType, service: HttpGeneralService, timeout: Timeout, geolocationService: GeolocationService)
  extends AbstractController[EventsScopeType](eventScope) with jsonHelper {

//  var events: js.Array[HappeningWithRelations] = new js.Array[HappeningWithRelations]
  var initLat =  0.0
  val coordinateMaxLength = 8

  geolocationService.getUserGeolocation map { geoloc =>
    timeout(() =>initLat = geoloc.lat.toString.substring(0, coordinateMaxLength).toDouble)
  }

  var initLng = 0.0

  geolocationService.getUserGeolocation map { geoloc =>
    timeout(() => initLng = geoloc.lng.toString.substring(0, coordinateMaxLength).toDouble)
  }

  def findMaybeSalableEventsContaining(pattern: String): Unit = {
    service.get(EventsRoutes.findMaybeSalableEvents(pattern)) map { foundEvents =>
      timeout(() => eventScope.maybeSalableEvents = read[Seq[MaybeSalableEvent]](foundEvents).toJSArray)
    }
  }

  def findById(id: Int): Unit = {
    service.get(EventsRoutes.find(id)) map { foundEvent =>
      timeout(() => eventScope.events = js.Array(read[HappeningWithRelations](foundEvent)))
    }
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

@js.native
trait EventsScopeType extends Scope {
  var events: js.Array[HappeningWithRelations] = js.native
  var maybeSalableEvents: js.Array[MaybeSalableEvent] = js.native
  var remove: js.Function1[HappeningWithRelations, _] = js.native
}