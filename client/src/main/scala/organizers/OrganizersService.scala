package organizers

import com.greencatsoft.angularjs.{Factory, Service, injectable}
import httpServiceFactory.HttpGeneralService
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON

@injectable("organizersService")
class OrganizersService(httpGeneralService: HttpGeneralService) extends Service with jsonHelper {

  def findByIdAsString(id: Int): Future[String] = httpGeneralService.get(OrganizersRoutes.findById(id))

  def findByIdAsJson(id: Int): Future[js.Any] =  findByIdAsString(id) map(organizer => JSON.parse(organizer))
}

@injectable("organizersService")
class OrganizersServiceFactory(httpGeneralService: HttpGeneralService) extends Factory[OrganizersService] {

  override def apply(): OrganizersService = new OrganizersService(httpGeneralService)
}
