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

  def formatEventDescription(eventDescription: String): String = {
    val linkPattern = """((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)""".r
    linkPattern.replaceAllIn(eventDescription
      .replaceAll("""\\n\\n""", " <br/><br/></div><div class='column large-12'>").replaceAll("""\\n""", " <br/>")
      .replaceAll("""\\t""", "    "), m => "<a href='http://" + m + "'>" + m + "</a>")
      .replaceAll("""\\n\\n""", " <br/><br/></div><div class='column large-12'>").replaceAll("""\\n""", " <br/>")
      .replaceAll("""\\t""", "    ").replaceAll("""</a>/""", "</a> ")
  }

  def addBannerToEventDescription(eventDescription: String, eventName: String, imgPath: String): String = {
    "<img class='width100p' src=" + imgPath  +
      "/><div class='columns large-12'><h2>" + eventName.replaceAll("\"", "") +
      "</h2></div><div id='desc'>" + formatEventDescription(eventDescription.substring(1).dropRight(1)) +
      "</div>"
  }

  def saveEvent(eventDescription: String, eventResp: Response, placeId: Long, imgPath: String) = {
    val eventJson = eventResp.json
    //println(s"$eventJson")

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


    val event: Event = new Event(-1L, facebookId, true, true, new Date, name, eventDescription, startTime, endTime, 16,
      List(), List(organizer), List(), List(), List(address))

    Event.save(event) match {
      case None => Event.update(event) //delete old imgs and insert news
      case Some(eventId) =>
        Address.saveAddressAndEventRelation(address, eventId)
        Place.saveEventPlaceRelation(eventId, placeId)
        if (imgPath.replaceAll("\"", "") != "null") Image.save(new Image(-1L, imgPath.replaceAll("\"", ""),
          Some(eventId), None))
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
          case Success(eventDetailed) =>  val description = Json.stringify(eventDetailed.json \ "description")
            val name = Json.stringify(eventDetailed.json \ "name")
            val imgPath = Json.stringify(eventDetailed.json \ "cover" \ "source")
            saveEvent(addBannerToEventDescription(description, name, imgPath), eventDetailed, placeId, imgPath)
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
