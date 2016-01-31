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

  def findById(id: Int): Unit = {
    service.get(EventsRoutes.find(id)) map { foundEvent =>
      timeout( () => events = js.Array(foundEvent))
    }
  }

  def find(offset: Int, numberToReturn: Int, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.find(offset, numberToReturn, geographicPoint)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def FindInHourInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.FindInHourInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def FindPassedInInterval(hourInterval: Int, lat: Double, lng: Double, offset: Int, numberToReturn: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.FindPassedInInterval(hourInterval, geographicPoint, offset, numberToReturn)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def findAllContaining(pattern: String, lat: Double, lng: Double): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(EventsRoutes.findAllContaining(pattern, geographicPoint)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def findByCityPattern(pattern: String): Unit = {
    service.get(EventsRoutes.findByCityPattern(pattern)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(EventsRoutes.findNearCity(city, numberToReturn, offset)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }

  def createByFacebookId(facebookId: String): Unit = {
    service.post(EventsRoutes.createByFacebookId(facebookId)) map { foundEvents =>
      timeout( () => events = js.Array(foundEvents))
    }
  }


}

@js.native
trait EventsScopeType extends Scope {
  var events: js.Array[Happening] = js.native
  var remove: js.Function1[Happening, _] = js.native
}