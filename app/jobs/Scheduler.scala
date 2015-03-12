package jobs

import java.util.Date
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import services.Utilities.normalizeUrl
import controllers.SearchArtistsController.getFacebookArtistsByWebsites

object Scheduler {
  val token = play.Play.application.configuration.getString("facebook.token")
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")
  val linkPattern = play.Play.application.configuration.getString("regex.linkPattern").r

  def start = {
    Place.findAllIdsAndFacebookIds.map { placeIdAndFacebookId: (Long, String) =>
      saveEventsOfPlace(placeIdAndFacebookId._1, placeIdAndFacebookId._2)
    }
  }

  def saveEventsOfPlace(placeId: Long, placeFacebookId: String) = {
    getEventsIdsByPlace(placeFacebookId).map {
      _.map { eventId =>
        WS.url("https://graph.facebook.com/v2.2/" + eventId)
          .withQueryString(
            "fields" -> "cover,description,name,start_time,end_time,owner,venue",
            "access_token" -> token)
          .get()
          .map { eventDetailed =>
          readFacebookEvent(eventDetailed) match {
            case Some(eventuallyFacebookEvent) =>
              eventuallyFacebookEvent map { saveEvent(_, placeId) }
            case None => println("Cannot create event: empty event created in readFacebookEvent")
          }
        }
      }
    }
  }

  def getEventsIdsByPlace(placeFacebookId: String): Future[Seq[String]] = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/")
      .withQueryString(
        "access_token" -> token)
      .get()
      .map { readEventsIdsFromResponse }
  }

  def saveEvent(facebookEvent: Event, placeId: Long) = {
    Event.save(facebookEvent) match {
      case None => Event.update(facebookEvent) //delete old imgs and insert news
      case Some(eventId) =>
        Place.saveEventPlaceRelation(eventId, placeId)
        facebookEvent.addresses.map { address =>
          Address.saveAddressAndEventRelation(address, eventId)
        }
    }
  }

  def readFacebookEvent(eventFacebookResponse: Response): Option[Future[Event]] = {
    val eventRead = (
        (__ \ "description").readNullable[String] and
        (__ \ "cover" \ "source").read[String] and
        (__ \ "name").read[String] and
        (__ \ "id").readNullable[String] and
        (__ \ "start_time").readNullable[String] and
        (__ \ "endTime").readNullable[String] and
        (__ \ "venue" \ "street").readNullable[String] and
        (__ \ "venue" \ "zip").readNullable[String] and
        (__ \ "venue" \ "city").readNullable[String] and
        (__ \ "owner" \ "id").readNullable[String]
      )((description: Option[String], source: String, name: String, facebookId: Option[String],
         startTime: Option[String], endTime: Option[String], street: Option[String], zip: Option[String],
         city: Option[String], maybeOwnerId: Option[String]) => {

        val eventuallyOrganizer = getOrganizerInfos(maybeOwnerId)
        val eventuallyAddress = getGeographicPoint(new Address(-1l, None, city, zip, street))
        val eventuallyMaybeArtists = getFacebookArtistsByWebsites(getWebsitesInDescription(description))

        for {
          organizer <- eventuallyOrganizer
          address <- eventuallyAddress
          artists <- eventuallyMaybeArtists
        } yield {
          val nonEmptyArtists = artists.flatten.toList
          val eventGenres = nonEmptyArtists.map { _.genres }.flatten
          new Event(-1L, facebookId, true, true, new Date(), name, None,
            formatDescription(description), formatDate(startTime).getOrElse(new Date()),
            formatDate(endTime), 16, List(new Image(-1L, source)), List(organizer).flatten,
            nonEmptyArtists, List(), List(address), List(), eventGenres)
        }
      })
    eventFacebookResponse.json.asOpt[Future[Event]](eventRead)
  }

  def readEventsIdsFromResponse(resp: Response): Seq[String] = {
    val readSoundFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readSoundFacebookIds).flatten
  }

  def formatDate(date: Option[String]): Option[Date] = date match {
    case Some(dateFound: String) => val date = dateFound.replace("T", " ")
      date.length match {
        case i if i <= 10 => Option(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(date))
        case i if i <= 13 => Option(new java.text.SimpleDateFormat("yyyy-MM-dd HH").parse(date))
        case _ => Option(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date))
      }
    case _ => None
  }

  def formatDescription(description: Option[String]): Option[String] = {
    //see if not faster to useGetWebsitesInDescription and after replace all matched?
    description match {
      case None => None
      case Some(desc) => Some("<div class='column large-12'>" +
        linkPattern.replaceAllIn(desc, m =>
          if (m.toString contains "@")
            "<a href='http://" + m + "'>ablezblazbelabezlabzlejbalejbazlejbajlez" + m + "</a>"
          else
            "<a href='http://" + m + "'>" + m + "</a>")
          .replaceAll( """\n\n""", "<br/><br/></div><div class='column large-12'>")
          .replaceAll( """\n""", "<br/>")
          .replaceAll( """\t""", "    ")
          .replaceAll( """</a>/""", "</a> ") +
        "</div>")
    }
  }

  def getWebsitesInDescription(maybeDescription: Option[String]): Set[String] = maybeDescription match {
    case None => Set.empty
    case Some(description) =>
      println(linkPattern.findAllIn(description).toSet.map { normalizeUrl })
      linkPattern.findAllIn(description).toSet.map { normalizeUrl }
  }

  def createNewImageIfSourceExists(source: Option[String]): List[Image] = source match {
    case Some(path) => List(new Image(-1, path))
    case None => List()
  }

  def splitArtistNamesInTitle(title: String): List[String] =
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "")

  def readOrganizer(organizer: Response, organizerId: String): Option[Organizer] = {
    val readOrganizer = (
      (__ \ "name").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "cover" \ "source").readNullable[String] and
        (__ \ "phone").readNullable[String] and
        (__ \ "public_transit").readNullable[String] and
        (__ \ "website").readNullable[String]
      ).apply((name: String, description: Option[String], source: Option[String], phone: Option[String],
               public_transit: Option[String], website: Option[String]) =>
      Organizer(-1L, Some(organizerId), name, formatDescription(description),
        phone, public_transit, website, verified = false,
        createNewImageIfSourceExists(source)))
    organizer.json.asOpt[Organizer](readOrganizer)
  }

  def getOrganizerInfos(maybeOrganizerId: Option[String]): Future[Option[Organizer]] = maybeOrganizerId match {
    case None => Future { None }
    case Some(organizerId) =>
      WS.url("https://graph.facebook.com/v2.2/" + organizerId)
        .withQueryString(
          "fields" -> "name,description,cover{source},location,phone,public_transit,website",
          "access_token" -> token)
        .get()
        .map { response => readOrganizer(response, organizerId) }
  }

  def readFacebookGeographicPoint = {
    /*val geographicPoint = (eventJson \ "venue" \ "latitude").as[Option[Float]] match {
      case Some(latitude) =>
        (eventJson \ "venue" \ "longitude").as[Option[Float]] match {
          case Some(longitude) => Some(s"($latitude,$longitude)")
          case _ => None
        }
      case _ => None
    }*/
  }

  def readGoogleGeographicPoint(googleGeoCodeResponse: Response): Option[String] = {
    val googleGeoCodeJson = googleGeoCodeResponse.json
    val latitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lat").asOpt[BigDecimal]
    val longitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lng").asOpt[BigDecimal]
    latitude match {
      case None => None
      case Some(lat) => longitude match {
        case None => None
        case Some(lng) => Option("(" + lat + "," + lng + ")")
      }
    }
  }

  def getGeographicPoint(address: Address): Future[Address] = {
    if (Vector(address.street, address.zip, address.city).flatten.length > 1) {
      WS.url("https://maps.googleapis.com/maps/api/geocode/json")
        .withQueryString( //replaceAll(" ", "+")) ??
          "address" -> (address.street.getOrElse("") + address.zip.getOrElse("") + address.city.getOrElse("")),
          "key" -> youtubeKey)
        .get()
        .map { response =>
        readGoogleGeographicPoint(response)  match {
          case Some(geographicPoint) => address.copy(geographicPoint = Option(geographicPoint))
          case None => address
        }
      }
    } else
      Future { address }
  }


  def getArtistsFromTitle(title: String): Set[String] = {
    /*val artistsFromTitle: List[String] = splitArtistNamesInTitle(name)
    println(artistsFromTitle)
    artistsFromTitle.map { artistName =>
     getFacebookArtist(artistName)
    }*/
    Set()
  }
}
