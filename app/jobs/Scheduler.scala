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
import models.Event.findEventOnFacebookByFacebookId
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

  def start(): Unit = Place.findAllAsTupleIdFacebookIdAndGeographicPoint.map {
    placeIdAndFacebookId: (Long, String, Option[String]) =>
      saveEventsOfPlace(placeIdAndFacebookId._1, placeIdAndFacebookId._2, placeIdAndFacebookId._3)
  }

  def saveEventsOfPlace(placeId: Long, placeFacebookId: String, placeGeographicPoint: Option[String]): Unit
  = getEventsIdsByPlace(placeFacebookId).map {
    _.map { eventId =>
      findEventOnFacebookByFacebookId(eventId) map { saveEvent(_, placeId, placeGeographicPoint) }
    }
  }

  def getEventsIdsByPlace(placeFacebookId: String): Future[Seq[String]] = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/")
      .withQueryString(
        "access_token" -> token)
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
        case None => Event.update(facebookEvent)
        case Some(eventId) =>
          Place.saveEventPlaceRelation(eventId, placeId)
          facebookEvent.addresses.map { address =>
            Address.saveAddressAndEventRelation(address, eventId)
          }
      }
    }
  }

  def readEventsIdsFromResponse(resp: Response): Seq[String] = {
    val readSoundFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readSoundFacebookIds).flatten
  }

  def splitArtistNamesInTitle(title: String): List[String] =
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "")

  def getArtistsFromTitle(title: String): Set[String] = {
    /*val artistsFromTitle: List[String] = splitArtistNamesInTitle(name)
    println(artistsFromTitle)
    artistsFromTitle.map { artistName =>
     getFacebookArtist(artistName)
    }*/
    Set.empty
  }
}
