package places


import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("placesController")
class PlacesController(scope: Scope, service: HttpGeneralService, timeout: Timeout)
  extends AbstractController[Scope](scope) with PlacesRoutes {
  var places: js.Array[String] = new js.Array[String]

  def findById(id: Int): Unit = {
    service.get(findByIdRoute(id.toLong: Long)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }

  def findContaining(pattern: String): Unit = {
    service.get(findContainingRoute(pattern: String)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(findNearCityRoute(city: String, numberToReturn: Int, offset: Int)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }

  def findNear(lat: Double, lng: Double, numberToReturn: Int, offset: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(findNearRoute(geographicPoint: String, numberToReturn: Int, offset: Int)) map { foundPlace =>
      timeout(() => places = js.Array(foundPlace))
    }
  }
}