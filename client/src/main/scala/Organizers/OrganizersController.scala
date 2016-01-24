package Organizers


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
    service.getJson(OrganizersRoutes.findById(id: Long)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }
  def findAllSinceOffset(offset: Long, numberToReturn: Long): Unit = {
    service.getJson(OrganizersRoutes.findAllSinceOffset(offset: Long, numberToReturn: Long)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }
  def findOrganizersContaining(pattern: String): Unit = {
    service.getJson(OrganizersRoutes.findOrganizersContaining(pattern: String)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }
  def findNearCity(city: String, numberToReturn: Int, offset: Int): Unit = {
    service.getJson(OrganizersRoutes.findNearCity(city: String, numberToReturn: Int, offset: Int)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }
  def findOrganizersNear(lat: Double, lng: Double, numberToReturn: Int, offset: Int): Unit = {
    val geographicPoint = lat + "," + lng
    service.getJson(OrganizersRoutes.findOrganizersNear(geographicPoint: String, numberToReturn: Int, offset: Int)) map { foundOrganizer =>
      timeout(() => organizers = js.Array(foundOrganizer))
    }
  }
}