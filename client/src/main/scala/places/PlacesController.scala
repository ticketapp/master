package places

import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService
import materialDesign.MdToastService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("placesController")
class PlacesController(scope: Scope, service: HttpGeneralService, placesService: PlacesService, timeout: Timeout,
                       mdToastService: MdToastService) extends AbstractController[Scope](scope) {
  var places: js.Any = Nil

  def findById(id: Int): Unit =
    placesService.findByIdAsJson(id) map(foundPlace => timeout(() => places = js.Array(foundPlace)))

  def findContaining(pattern: String): Unit = {
    service.get(PlacesRoutes.findContaining(pattern: String)) map { foundPlace =>
      timeout(() => places = JSON.parse(foundPlace))
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(PlacesRoutes.findNearCity(city: String, numberToReturn: Int, offset: Int)) map { foundPlace =>
      timeout(() => places = JSON.parse(foundPlace))
    }
  }

  def findNear(lat: Double, lng: Double, numberToReturn: Int, offset: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(PlacesRoutes.findNear(geographicPoint: String, numberToReturn: Int, offset: Int)) map { foundPlace =>
      timeout(() => places = JSON.parse(foundPlace))
    }
  }

  def deleteEventRelation(eventId: Int, placeId: Int): Unit = {
    service.delete(PlacesRoutes.deleteEventRelation(eventId, placeId)) map { result =>
      val toast = mdToastService.simple("relation deleted")
      mdToastService.show(toast)
    }
  }

  def saveEventRelation(eventId: Int, placeId: Int): Unit = {
    service.post(PlacesRoutes.saveEventRelation(eventId, placeId)) map { result =>
      val toast = mdToastService.simple("relation added")
      mdToastService.show(toast)
    }
  }
}