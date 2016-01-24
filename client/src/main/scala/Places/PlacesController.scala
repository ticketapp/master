package Places


import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("placesController")
class PlacesController(scope: Scope, service: HttpGeneralService, timeout: Timeout)
  extends AbstractController[Scope](scope) {
  var places: js.Array[String] = new js.Array[String]

  def findById(id: Long): Unit = {
    service.getJson(PlacesRoutes.findById(id: Long)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }
  def findAllSinceOffset(offset: Long, numberToReturn: Long): Unit = {
    service.getJson(PlacesRoutes.findAllSinceOffset(offset: Long, numberToReturn: Long)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }
  def findPlacesContaining(pattern: String): Unit = {
    service.getJson(PlacesRoutes.findPlacesContaining(pattern: String)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }
  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.getJson(PlacesRoutes.findNearCity(city: String, numberToReturn: Int, offset: Int)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }
  def findPlacesNear(lat: Double, lng: Double, numberToReturn: Int, offset: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.getJson(PlacesRoutes.findPlacesNear(geographicPoint: String, numberToReturn: Int, offset: Int)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }

}