package jobs

import models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import javax.inject.Inject

class Scheduler @Inject()(val eventMethods: EventMethods,
                          val placeMethods: PlaceMethods) {

//  def start(): Unit = placeMethods.findAllWithFacebookId map { _ map { place =>
//    eventMethods.getEventsFacebookIdByPlace(place.facebookId.get) map { _.map { eventId =>
//      eventMethods.findEventOnFacebookByFacebookId(eventId) map {
//        saveEventWithGeographicPointAndPlaceRelation(_, place.id.get, place.geographicPoint)
//      }
//    }}
//  }}
//
//  def saveEventWithGeographicPointAndPlaceRelation(facebookEvent: Event, placeId: Long,
//                                                   placeGeographicPoint: Option[String]): Unit = {
//    eventMethods.save(facebookEvent.copy(geographicPoint = placeGeographicPoint)) match {
//      case Some(eventId) =>
//        placeMethods.saveEventRelation(eventId, placeId)
//      case _ =>
//        Logger.error("Scheduler.saveEventWithGeographicPointAndPlaceRelation: event not saved")
//    }
//  }
}
