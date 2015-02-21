package jobs

import java.util.Date
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import scala.util.{Failure, Success}
import controllers.WebServiceException

object Scheduler {
  val token = play.Play.application.configuration.getString("facebook.token")
  val linkPattern = play.Play.application.configuration.getString("regex.linkPattern").r

  def formatEventDescription(eventDescription: String): String = {
    "<div class='columns large-12'>" + linkPattern.replaceAllIn(eventDescription
      .replaceAll("""\\n\\n""", " <br/><br/></div><div class='column large-12'>").replaceAll("""\\n""", " <br/>")
      .replaceAll("""\\t""", "    "), m => "<a href='http://" + m + "'>" + m + "</a>")
        .replaceAll("""\\n\\n""", " <br/><br/></div><div class='column large-12'>").replaceAll("""\\n""", " <br/>")
        .replaceAll("""\\t""", "    ").replaceAll("""</a>/""", "</a> ").substring(1).dropRight(1) + "</div>"
  }

  def saveEvent(eventDescription: String, eventResp: Response, placeId: Long, imgPath: String) = {
    val eventJson = eventResp.json

    val geographicPoint = (eventJson \ "venue" \ "latitude").as[Option[Float]] match {
      case Some(latitude) =>
        (eventJson \ "venue" \ "longitude").as[Option[Float]] match {
          case Some(longitude) => Some(s"$latitude, $longitude")
          case _ => None
        }
      case _ => None
    }
    val street = (eventJson \ "venue" \ "street").as[Option[String]]
    val zip = (eventJson \ "venue" \ "zip").as[Option[String]]
    val city = (eventJson \ "venue" \ "city").as[Option[String]]
    val address: Address = new Address(-1l, true, false, geographicPoint, city, zip, street) // + tester si
    //l'adresse est vide

    val name = Json.stringify(eventJson \ "name").replaceAll("\"", "")
    val facebookId = Some(Json.stringify(eventJson \ "id").replaceAll("\"", ""))
    //println(facebookId) 783881178345234

    val startTimeString = Json.stringify(eventJson \ "start_time").replaceAll("\"", "").replace("T", " ")
    val startTime = startTimeString.length match {
      case i if i <= 10 => new java.text.SimpleDateFormat("yyyy-MM-dd").parse(startTimeString)
      case i if i <= 13 => new java.text.SimpleDateFormat("yyyy-MM-dd HH").parse(startTimeString)
      case _ => new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(startTimeString)
    }

    val endTimeString = (eventJson \ "end_time").as[Option[String]]
    val endTime = endTimeString match {
      case Some(a) => a.length match {
        case i if i <= 10 => Some(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(startTimeString))
        case i if i <= 13 => Some(new java.text.SimpleDateFormat("yyyy-MM-dd HH").parse(startTimeString))
        case _ => Some(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(startTimeString))
      }
      case _ => None
    }

    val readOwnerName = Json.stringify(eventJson \ "owner" \ "name").replaceAll("\"", "")
    val readOwnerId = Json.stringify(eventJson \ "owner" \ "id").replaceAll("\"", "")
    val organizer = new Organizer(-1L, new Date, Some(readOwnerId), readOwnerName)

    var event: Event = new Event(-1L, facebookId, true, true, new Date, name, eventDescription, startTime, endTime, 16,
      List(), List(organizer), List(), List(), List(address))

    imgPath.replaceAll("\"", "") match {
      case "null" =>
      case _ => event = event.copy( images = List(new Image(-1L, imgPath.replaceAll("\"", ""))))
      }

    Event.save(event) match {
      case None => Event.update(event) //delete old imgs and insert news
      case Some(eventId) =>
        Address.saveAddressAndEventRelation(address, eventId)
        Place.saveEventPlaceRelation(eventId, placeId)
    }
  }

  def returnListOfIdsFromEvents(resp : Response): List[String] = {
    var ids: List[String] = List()
    val responseDataIds = resp.json \ "data" \\ "id"
    for(j <- responseDataIds) {
      ids = ids :+ Json.stringify(j).replaceAll("\"", "")
    }
    ids
  }

  def saveEventsOfPlace(placeId: Long, placeFacebookId: String) = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/?access_token=" +
      token).get onComplete {
      case Success(events) => returnListOfIdsFromEvents(events).map( eventId =>
        WS.url("https://graph.facebook.com/v2.2/" + eventId +
          "?fields=cover,description,name,start_time,end_time,owner,venue" + "&access_token=" + token)
          .get onComplete {
          /*
          val readName: Reads[String] = (__ \ "name").read[String]
    val readCategory: Reads[String] = (__ \ "category").read[String]
    val readId: Reads[String] = (__ \ "id").read[String]
    val readCoverSource: Reads[String] = (__ \ "source").read[String]
    val readOptionalCover: Reads[Option[String]] = (__ \ "cover").readNullable(readCoverSource)
    val readWebsites: Reads[Option[String]] = (__ \ "website").readNullable
    val readLink: Reads[String] = (__ \ "link").read[String]
    val readAllArtist: Reads[(String, String, String, Option[String], Option[String], String)] =
      readName.and(readId).and(readCategory).and(readOptionalCover).and(readWebsites).and(readLink)
        .apply((name: String, id: String, category: String, maybeCover: Option[String], website: Option[String], link: String)
      => (name, id, category, maybeCover, website, link))
    val readArtistsArray: Reads[Seq[(String, String, String, Option[String], Option[String], String)]] = Reads.seq(readAllArtist)
    val collectOnlyMusiciansWithCover: Reads[Seq[(String, String, String, Option[String], String)]] = readArtistsArray.map {
      pages =>
        pages.collect{ case (name, id, "Musician/band", Some(cover), websites, link) => (name, id, cover, websites, link) }
    }
    val readArtists: Reads[Seq[FacebookArtist]] = collectOnlyMusiciansWithCover.map { artists =>
      artists.map{ case (name, id, cover, websites, link) =>
        FacebookArtist(name, id, cover, websitesStringToWebsitesList(websites), link)
      }
    }
           */
          case Success(eventDetailed) =>
            val description = eventDetailed.json.as[String]((__ \ "description").read[String])
            val imgPath = eventDetailed.json.as[String]((__ \ "cover" \ "source").read[String])
            saveEvent(formatEventDescription(description), eventDetailed, placeId, imgPath)
          case Failure(f) => throw new WebServiceException("An error has occurred in saveEventsOfPlace: " + f.getMessage)
        } )
      case Failure(f) => throw new WebServiceException("An error has occurred in saveEventsOfPlace: " + f.getMessage)
    }
  }

  def start() = {
    Place.findAllIdsAndFacebookIds match {
      case Failure(f) => throw new WebServiceException("Error in scheduler : " + f.getMessage)
      case Success(listPlacesIdAndFbIdFromDatabase) =>
        listPlacesIdAndFbIdFromDatabase.foreach( placeIdAndFbId =>
          saveEventsOfPlace(placeIdAndFbId._1, placeIdAndFbId._2)
        )
    }
  }
}
