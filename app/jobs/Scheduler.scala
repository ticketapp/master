package jobs

import java.util.Date
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import services.Utilities._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import controllers.WebServiceException

//import controllers.SearchArtistsController._

import play.api.libs.functional.syntax._

object Scheduler {
  val token = play.Play.application.configuration.getString("facebook.token")
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")
  val linkPattern = play.Play.application.configuration.getString("regex.linkPattern").r

  def start() = {
    Place.findAllIdsAndFacebookIds.map { findAllIdsAndFacebookId: (Long, String) =>
      saveEventsOfPlace(findAllIdsAndFacebookId._1, findAllIdsAndFacebookId._2)
    }
  }

  def saveEventsOfPlace(placeId: Long, placeFacebookId: String) = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/")
      .withQueryString(
        "access_token" -> token
      )
      .get()
      .map { readEventsIdsFromResponse(_)
      .map { eventId =>
        WS.url("https://graph.facebook.com/v2.2/" + eventId)
          .withQueryString(
            "fields" -> "cover,description,name,start_time,end_time,owner,venue",
            "access_token" -> token
          )
          .get()
          .map { eventDetailed =>
            val description = eventDetailed.json.asOpt[String]((__ \ "description").read[String])
            val imgPath = eventDetailed.json.as[String]((__ \ "cover" \ "source").read[String])
            saveEvent(formatDescription(description), eventDetailed, placeId, imgPath)
          }
      }
    }
  }

  def readEventsIdsFromResponse(resp: Response): Seq[String] = {
    val readSoundFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readSoundFacebookIds)
      .flatten
  }

  def saveEvent(eventDescription: Option[String], eventResp: Response, placeId: Long, imgPath: String) = {
    val eventJson = eventResp.json
    //println(eventJson)

    val address = readAddressFromEvent(eventJson)
    val geographicPoint = None

    val name = eventJson.as[String]((__ \ "name").read[String])
    /*val artistsFromTitle: List[String] = splitArtistNamesInTitle(name)
    println(artistsFromTitle)
    artistsFromTitle.map { artistName =>
      getFacebookArtist(artistName)
    }*/


    val facebookId = eventJson.asOpt[String]((__ \ "id").read[String]) //println(facebookId) 783881178345234
    val startTime = formatDate(eventJson \ "start_time")
    val endTime = formatDate(eventJson \ "end_time")
    val readOwnerId = eventJson.as[String]((__ \ "owner" \ "id").read[String])

    getOrganizerInfos(readOwnerId).map { organizer =>
      var event: Event = new Event(-1L, facebookId, true, true, new Date, name, geographicPoint, eventDescription,
        startTime.getOrElse(new Date()), endTime, 16, List(), organizer, List(), List(), List(address))

      imgPath match {
        case "null" =>
        case _ => event = event.copy(images = List(new Image(-1L, imgPath)))
      }

      Event.save(event) match {
        case None => Event.update(event) //delete old imgs and insert news
        case Some(eventId) =>
          //println(eventId + event.images.toString())
          Address.saveAddressAndEventRelation(address, eventId)
          Place.saveEventPlaceRelation(eventId, placeId)
      }
    }
  }

  /*def getFacebookArtist(artistName: String, webSites): Option[Artist] = {

  }*/

  def readAddressFromEvent(eventJson: JsValue): Address = {
    //+ tester si
    //l'adresse est vide
    val street = (eventJson \ "venue" \ "street").as[Option[String]]
    val zip = (eventJson \ "venue" \ "zip").as[Option[String]]
    val city = (eventJson \ "venue" \ "city").as[Option[String]]
    val geographicPoint = None

    new Address(-1l, geographicPoint, city, zip, street)
  }

  def formatDate(date: JsValue): Option[Date] = date.asOpt[String] match {
    case Some(dateFound: String) => val date = dateFound.replace("T", " ")
      date.length match {
        case i if i <= 10 => Option(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(date))
        case i if i <= 13 => Option(new java.text.SimpleDateFormat("yyyy-MM-dd HH").parse(date))
        case _ => Option(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date))
      }
    case _ => None
  }

  def formatDescription(description: Option[String]): Option[String] = {
    //déjà match tous
    //ensuite formate description avec websites en plus comme arg de formatEventDescription
    //et une pour enregistrer les websites
    description match {
      case None => None
      case Some(desc) => Some("<div class='column large-12'>" +
        linkPattern.replaceAllIn(desc, m => "<a href='http://" + m + "'>" + m + "</a>")
          .replaceAll( """\n\n""", "<br/><br/></div><div class='column large-12'>")
          .replaceAll( """\n""", "<br/>")
          .replaceAll( """\t""", "    ")
          .replaceAll( """</a>/""", "</a> ") + "</div>")
    }
  }

  def createNewImageIfSourceExists(source: Option[String]): List[Image] = {
    source match {
      case Some(path) => List(new Image(-1, path))
      case None => List()
    }
  }

  def splitArtistNamesInTitle(title: String): List[String] = {
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "")
  }


  def getOrganizerInfos(organizerId: String): Future[List[Organizer]] = {
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

    WS.url("https://graph.facebook.com/v2.2/" + organizerId +
      "?fields=name,description,cover%7Bsource%7D,location,phone,public_transit,website&access_token=" + token).get()
      .map { organizer =>
      organizer.json.asOpt[Organizer](readOrganizer).toList
    }
  }

  def getGeographicPoint(street: Option[String], zip: Option[String], city: Option[String]): Future[String] = {
    /*val geographicPoint = (eventJson \ "venue" \ "latitude").as[Option[Float]] match {
          case Some(latitude) =>
            (eventJson \ "venue" \ "longitude").as[Option[Float]] match {
              case Some(longitude) => Some(s"($latitude,$longitude)")
              case _ => None
            }
          case _ => None
        }*/
    WS.url("https://maps.googleapis.com/maps/api/geocode/json?address=" +
      normalizeString(street.getOrElse("").replaceAll(" ", "+")) +
      normalizeString(zip.getOrElse("").replaceAll(" ", "+")) +
      normalizeString(city.getOrElse("").replaceAll(" ", "+")) +
      "&key=" + youtubeKey).get().map { geographicPoint =>

      //verify JsUndefined :
      /*(geographicPoint.json \ "results")(0) \ "geometry" \ "location" \ "lat" +
      (geographicPoint.json \ "results")(0) \ "geometry" \ "location" \ "lng"*/
      ""
    }

  }
}
