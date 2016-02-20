package organizers

import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll


@injectable("organizersController")
class OrganizersController(scope: Scope, service: HttpGeneralService, timeout: Timeout)
  extends AbstractController[Scope](scope) {
  var organizers: js.Array[String] = new js.Array[String]

  def findById(id: Long): Unit = {
    service.get(OrganizersRoutes.findById(id: Long)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }

  def find(offset: Long, numberToReturn: Long): Unit = {
    service.get(OrganizersRoutes.findAllSinceOffset(offset: Long, numberToReturn: Long)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }

  def findContaining(pattern: String): Unit = {
    service.get(OrganizersRoutes.findContaining(pattern: String)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.get(OrganizersRoutes.findNearCity(city: String, numberToReturn: Int, offset: Int)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }

  def findNear(lat: Double, lng: Double, numberToReturn: Int, offset: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.get(OrganizersRoutes.findNear(geographicPoint: String, numberToReturn: Int, offset: Int)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }
}