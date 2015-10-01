package jobs

import java.util.Date
import controllers.SchedulerException
import models.Genre._
import play.api.Logger
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import services.SearchSoundCloudTracks._
import services.SearchYoutubeTracks._
import services.Utilities
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import scala.util.matching.Regex
import services.Utilities.{ normalizeUrl, normalizeString, getNormalizedWebsitesInText }
import controllers.SearchArtistsController.{ getEventuallyArtistsInEventTitle, getFacebookArtistsByWebsites }
import services.Utilities.{ facebookToken, googleKey }

object Scheduler {

  def start(): Unit = {
    findEventsForPlaces
    findEventsForOrganizers
  }
  
  def findEventsForOrganizers: Unit = {
    Organizer.findAllWithFacebookId map {
      _ map { organizer =>
        Event.getEventsFacebookIdByPlaceOrOrganizerFacebookId(organizer.facebookId.get) map {
          _.map { eventId =>
            Event.findEventOnFacebookByFacebookId(eventId)
          }
        }
        
      }
    }
  }

  def findEventsForPlaces: Unit = {
    Place.findAllWithFacebookId map {
      _ map { place =>
        Event.getEventsFacebookIdByPlaceOrOrganizerFacebookId(place.facebookId.get) map {
          _.map { eventId =>
            Event.findEventOnFacebookByFacebookId(eventId) map {
              saveEventWithGeographicPointAndPlaceRelation(_, place.placeId.get, place.geographicPoint)
            }
          }
        }
      }
    }
  }

  def saveEventWithGeographicPointAndPlaceRelation(facebookEvent: Event, placeId: Long,
                                                   placeGeographicPoint: Option[String]): Unit = {
    Event.save(facebookEvent.copy(geographicPoint = placeGeographicPoint)) match {
      case Some(eventId) =>
        Place.saveEventRelation(eventId, placeId)
      case _ =>
        Logger.error("Scheduler.saveEventWithGeographicPointAndPlaceRelation: event not saved")
    }
  }
}
