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

object Scheduler {
  val token = play.Play.application.configuration.getString("facebook.token")
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")

  def start(): Unit = Place.findAllWithFacebookId map { _ map { place =>
    getEventsIdsByPlace(place.facebookId.get) map { _.map { eventId =>
      Event.findEventOnFacebookByFacebookId(eventId) map {
        saveEvent(_, place.placeId.get, place.geographicPoint)
      }
    }}
  }}

  def getEventsIdsByPlace(placeFacebookId: String): Future[Seq[String]] = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/")
      .withQueryString("access_token" -> token)
      .get()
      .map { readEventsIdsFromResponse }
  }

  def saveEvent(facebookEvent: Event, placeId: Long, placeGeographicPoint: Option[String]) = {
    val organizerAddressWithGeographicPoint = Address.getGeographicPoint(facebookEvent.organizers.head.address.get)
    organizerAddressWithGeographicPoint.map { organizerAddress =>
      val organizerWithGeographicPoint = facebookEvent.organizers.head.copy(address = Option(organizerAddress),
        geographicPoint = organizerAddress.geographicPoint)

      Event.save(facebookEvent.copy(geographicPoint = placeGeographicPoint,
        organizers = List(organizerWithGeographicPoint))) match {
        case None =>
          Event.update(facebookEvent)
        case Some(eventId) =>
          Place.saveEventRelation(eventId, placeId)
          facebookEvent.addresses.map { address => Address.saveAddressAndEventRelation(address, eventId) }
      }
    }
  }

  def readEventsIdsFromResponse(resp: Response): Seq[String] = {
    val readSoundFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readSoundFacebookIds).flatten
  }

  def splitArtistNamesInTitle(title: String): List[String] =
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "")
}
