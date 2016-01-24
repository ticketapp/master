package events


import com.greencatsoft.angularjs.core.{Timeout, Scope}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService

import scala.scalajs.js
import scala.scalajs.js.JSConverters.array2JSRichGenTrav
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("eventsController")
class EventsController(scope: EventsScopeType, service: HttpGeneralService, timeout: Timeout)
  extends AbstractController[EventsScopeType](scope) {
  var events: js.Array[String] = new js.Array[String]

  def findEventById(id: Int): Unit = {
    service.getJson(EventsRoutes.find(id)) map { foundEvent =>
      timeout( () => events = js.Array(foundEvent))
    }
  }

  def findEvents(offset: Int, numberToReturn: Int, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.getJson(EventsRoutes.events(offset, numberToReturn, geographicPoint)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def eventsInHourInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.getJson(EventsRoutes.eventsInHourInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def eventsPassedInInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.getJson(EventsRoutes.eventsPassedInInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def findAllContaining(pattern: String, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.getJson(EventsRoutes.findAllContaining(pattern, geographicPoint)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def findByCityPattern(pattern: String): Unit = {
    service.getJson(EventsRoutes.findByCityPattern(pattern)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.getJson(EventsRoutes.findNearCity(city, numberToReturn, offset)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def createEventByFacebookId(facebookId: String): Unit = {
    service.postJsonAndRead(EventsRoutes.createEventByFacebookId(facebookId)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }


}

@js.native
trait EventsScopeType extends Scope {
  var events: js.Array[Happening] = js.native
  var remove: js.Function1[Happening, _] = js.native
}