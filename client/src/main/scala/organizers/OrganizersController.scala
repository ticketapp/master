package organizers

import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService
import materialDesign.MdToastService
import places.PlacesRoutes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("organizersController")
class OrganizersController(scope: Scope, service: HttpGeneralService, timeout: Timeout, mdToastService: MdToastService)
  extends AbstractController[Scope](scope) {
  var organizers: js.Any = Nil

  def findById(id: Long): Unit = {
    service.get(OrganizersRoutes.findById(id: Long)) map { foundOrganizer =>
      timeout(() => organizers = JSON.parse(foundOrganizer))
    }
  }

  def find(offset: Long, numberToReturn: Long): Unit = {
    service.get(OrganizersRoutes.findAllSinceOffset(offset: Long, numberToReturn: Long)) map { foundOrganizer =>
      timeout(() => organizers = JSON.parse(foundOrganizer))
    }
  }

  def findContaining(pattern: String): Unit = {
    println(pattern)
    service.get(OrganizersRoutes.findContaining(pattern: String)) map { foundOrganizer =>
      timeout(() => organizers = JSON.parse(foundOrganizer))
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(OrganizersRoutes.findNearCity(city: String, numberToReturn: Int, offset: Int)) map { foundOrganizer =>
      timeout(() => organizers = JSON.parse(foundOrganizer))
    }
  }

  def findNear(lat: Double, lng: Double, numberToReturn: Int, offset: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(OrganizersRoutes.findNear(geographicPoint: String, numberToReturn: Int, offset: Int)) map { foundOrganizer =>
      timeout(() => organizers = JSON.parse(foundOrganizer))
    }
  }


  def deleteEventRelation(eventId: Int, organizerId: Int): Unit = {
    service.delete(OrganizersRoutes.deleteEventRelation(eventId, organizerId)) map { result =>
      val toast = mdToastService.simple("relation deleted")
      mdToastService.show(toast)
    }
  }

  def saveEventRelation(eventId: Int, organizerId: Int): Unit = {
    service.post(OrganizersRoutes.saveEventRelation(eventId, organizerId)) map { result =>
      val toast = mdToastService.simple("relation added")
      mdToastService.show(toast)
    }
  }
}