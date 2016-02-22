package events

import com.greencatsoft.angularjs.{Factory, Service, injectable}
import httpServiceFactory.HttpGeneralService
import upickle.default._
import utilities.jsonHelper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON

@injectable("eventsService")
class EventsService(httpGeneralService: HttpGeneralService) extends Service with jsonHelper {

  def findByIdAsString(id: Int): Future[String] = httpGeneralService.get(EventsRoutes.findById(id))

  def findByIdAsJson(id: Int): Future[js.Any] =  findByIdAsString(id) map(event => JSON.parse(event))

  def findById(id: Int): Future[HappeningWithRelations] =
    findByIdAsString(id) map(foundEvent => read[HappeningWithRelations](foundEvent))
}

@injectable("eventsService")
class EventsServiceFactory(httpGeneralService: HttpGeneralService) extends Factory[EventsService] {

  override def apply(): EventsService = new EventsService(httpGeneralService)
}
