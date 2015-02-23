package jobs

import java.util.Date
import controllers.ArtistController.FacebookArtist
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import controllers.{ArtistController, WebServiceException}
import play.api.libs.functional.syntax._

object Scheduler {
  val token = play.Play.application.configuration.getString("facebook.token")
  val linkPattern = play.Play.application.configuration.getString("regex.linkPattern").r

  def formatDescription(description: Option[String]): Option[String] = {
    description match {
      case None => None
      case Some(desc) => Some("<div class='column large-12'>" +
        linkPattern.replaceAllIn(desc, m => "<a href='http://" + m + "'>" + m + "</a>")
        .replaceAll( """\n\n""", "<br/><br/></div><div class='column large-12'>")
        .replaceAll( """\n""", "<br/>")
        .replaceAll( """\t""", "    ").replaceAll( """</a>/""", "</a> ") + "</div>")
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
          createNewImageIfSourceExists(source)) )

    WS.url("https://graph.facebook.com/v2.2/" + organizerId +
      "?fields=name,description,cover%7Bsource%7D,location,phone,public_transit,website&access_token=" + token).get()
      .map { organizer =>
        organizer.json.asOpt[Organizer](readOrganizer).toList
      }
  }

  def saveEvent(eventDescription: Option[String], eventResp: Response, placeId: Long, imgPath: String) = {
    val eventJson = eventResp.json

    val geographicPoint = (eventJson \ "venue" \ "latitude").as[Option[Float]] match {
      case Some(latitude) =>
        (eventJson \ "venue" \ "longitude").as[Option[Float]] match {
          case Some(longitude) => Some(s"($latitude,$longitude)")
          case _ => None
        }
      case _ => None
    }
    val street = (eventJson \ "venue" \ "street").as[Option[String]]
    val zip = (eventJson \ "venue" \ "zip").as[Option[String]]
    val city = (eventJson \ "venue" \ "city").as[Option[String]]
    val address: Address = new Address(-1l, geographicPoint, city, zip, street)
     // + tester si
    //l'adresse est vide

    val name = eventJson.as[String]((__ \ "name").read[String])



    val listArtistsFromTitle: List[String] = splitArtistNamesInTitle(name)
    println(listArtistsFromTitle)





    val facebookId = Some(eventJson.as[String]((__ \ "id").read[String]))
    //println(facebookId) 783881178345234

    val startTimeString = eventJson.as[String]((__ \ "start_time").read[String]).replace("T", " ")
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

    val readOwnerId = eventJson.as[String]((__ \ "owner" \ "id").read[String])
    getOrganizerInfos(readOwnerId).map { organizer =>
      var event: Event = new Event(-1L, facebookId, true, true, new Date, name, geographicPoint, eventDescription,
        startTime, endTime, 16, List(), organizer, List(), List(), List(address))

      imgPath match {
        case "null" =>
        case _ => event = event.copy( images = List(new Image(-1L, imgPath)))
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

  def returnListOfIdsFromEvents(resp : Response): Seq[String] = {
    val readSoundFacebookIds: Reads[Seq[String]] = Reads.seq((__ \ "id").read[String])
    (resp.json \ "data").as[Seq[String]](readSoundFacebookIds)
  }

  def saveEventsOfPlace(placeId: Long, placeFacebookId: String) = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/?access_token=" +
      token).get onComplete {
      case Success(events) => returnListOfIdsFromEvents(events).map( eventId =>
        WS.url("https://graph.facebook.com/v2.2/" + eventId +
          "?fields=cover,description,name,start_time,end_time,owner,venue" + "&access_token=" + token)
          .get onComplete {
          case Success(eventDetailed) =>
            val description = eventDetailed.json.asOpt[String]((__ \ "description").read[String])
            val imgPath = eventDetailed.json.as[String]((__ \ "cover" \ "source").read[String])
            saveEvent(formatDescription(description), eventDetailed, placeId, imgPath)
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
