package places

import com.greencatsoft.angularjs.{Factory, Service, injectable}
import httpServiceFactory.HttpGeneralService
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON

@injectable("placesService")
class PlacesService(httpGeneralService: HttpGeneralService) extends Service with jsonHelper {

  def findByIdAsString(id: Int): Future[String] = httpGeneralService.get(PlacesRoutes.findById(id))

  def findByIdAsJson(id: Int): Future[js.Any] =  findByIdAsString(id) map(place => JSON.parse(place))
}

@injectable("placesService")
class PlacesServiceFactory(httpGeneralService: HttpGeneralService) extends Factory[PlacesService] {

  override def apply(): PlacesService = new PlacesService(httpGeneralService)
}
