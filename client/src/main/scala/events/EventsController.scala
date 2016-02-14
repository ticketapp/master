package events

import com.greencatsoft.angularjs.core.{Timeout, Scope}
import com.greencatsoft.angularjs.{AbstractController, injectable}
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
class EventsController(eventScope: EventsScopeType, service: HttpGeneralService, timeout: Timeout)
  extends AbstractController[EventsScopeType](eventScope) with jsonHelper {

  var events: js.Array[HappeningWithRelations] = new js.Array[HappeningWithRelations]

  def findById(id: Int): Unit = {
    service.get(EventsRoutes.find(id)) map { foundEvent =>
      timeout(() => events = js.Array(read[HappeningWithRelations](foundEvent)))
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
      timeout(() => events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findPassedInInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.FindPassedInInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout(() => events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findAllContaining(pattern: String, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.findAllContaining(pattern, geographicPoint)) map { foundEvents =>
      timeout(() => events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findByCityPattern(pattern: String): Unit = {
    service.get(EventsRoutes.findByCityPattern(pattern)) map { foundEvents =>
      timeout(() => events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(EventsRoutes.findNearCity(city, numberToReturn, offset)) map { foundEvents =>
      timeout(() => events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }

  def createByFacebookId(facebookId: String): Unit = {
    service.post(EventsRoutes.createByFacebookId(facebookId)) map { foundEvents =>
      timeout(() => events = read[Seq[HappeningWithRelations]](foundEvents).toJSArray)
    }
  }
}

@js.native
trait EventsScopeType extends Scope {
  var events: js.Array[HappeningWithRelations] = js.native
  var remove: js.Function1[HappeningWithRelations, _] = js.native
}