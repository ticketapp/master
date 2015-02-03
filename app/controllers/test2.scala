package controllers

import java.io.{IOException, FileNotFoundException}
import java.util.Date
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Place, Event}
import scala.io.Source
import play.api.mvc.Results._

import scala.util.{Failure, Success, Try}

object Test2 extends Controller {
  var token = "CAAJZCFQiqd2cBAMb4duQetTO168VgGEaF3N06HIueS6b1zE6hdt7FG2z4MwOwxSl9M4Yiy2pGZCMHHXuTQ7VFfjah3AxFJG9KujqoaJrD5PuKcI29BC6YdZBxalX3dw4KlKdmmYmHn7v2Eq8kQ4i8T2kyAMOs3d6o4jhxZCEPtg6hDL3nNJ3FjlDedMxPJKevZBGXAOmqoSE7gV2dVIB0olaNeU9TaiQZD"

  def formatEventDescription(eventDescription: String): String = {
    val eventDesc = eventDescription.replaceAll("""\\n\\n""", " <br/><br/></div><div class='column large-12'>")
      .replaceAll("""\\n""", " <br/>").replaceAll("""\\t""", "    ")

    val linkPattern = """((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)""".r
    linkPattern.replaceAllIn(eventDesc, m => "<a href='" + m.group(0) + "'>" + m.group(0) + "</a>")
  }

  def addBannerToEventDescription(eventDescription: String, eventName: String, imgPath: String): String = {
    "<img class='width100p' src=" + imgPath  +
      "/><div class='columns large-12'><h2>" + eventName.replaceAll("\"", "") +
      "</h2></div><div class='columns large-12'>" +  formatEventDescription(eventDescription.substring(1).dropRight(1)) +
      "</div>"
  }

  def saveEvent(eventDescription: String, eventResp: Response, placeId: Long) = {
    val eventJson = eventResp.json
    //println(eventJson)

    val name = Json.stringify(eventJson \ "name").replaceAll("\"", "")
    val facebookId = Some(Json.stringify(eventJson \ "id").replaceAll("\"", ""))
    val startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
      .parse(Json.stringify(eventJson \ "start_time").replaceAll("\"", "")
      .replace("T", " ").replace(""":\d\d+\d\d\d""", ""))
    var endTime: Option[Date] = None

    (eventJson \ "end_time").as[Option[String]] match {
      case Some(a) => endTime = Some(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
        .parse(Json.stringify(eventJson \ "end_time").replaceAll("\"", "")
        .replace("T", " ").replace(""":\d\d+\d\d\d""", "")))
      case _ =>
    }

    val eventId = Event.save(new Event(-1L, facebookId, true, true, new Date, name, None, None,
      eventDescription, startTime, endTime, 16, List(), List(), List(), List()))

    Event.saveEventPlaceRelation(eventId, placeId)
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
    for {
      events <- WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/?access_token=" + token).get
      _ = println(events.json)

      eventsId = returnListOfIdsFromEvents(events)
      //_ = println(eventsId)

      listOfEvents <- Future.sequence(eventsId.map( eventId =>
        WS.url("https://graph.facebook.com/v2.2/" + eventId +
          "?fields=cover,description,name,start_time,end_time,owner" + "&access_token=" + token
        ).get) )
      _ = listOfEvents.foreach(x => println(x.json))

    } yield  {
      listOfEvents.map(response => {
        val description = Json.stringify(response.json \ "description")
        val name = Json.stringify(response.json \ "name")
        val imgPath = Json.stringify(response.json \ "cover" \ "source")
        saveEvent(addBannerToEventDescription(description, name, imgPath), response, placeId)
      } )
    }
  }

  def test2 = Action {
    Place.findAllIdsAndFacebookIds match {
      case Failure(f) => Ok("Il y a eu une erreur :\n" + f + "\n")
      case Success(listPlacesIdAndFbIdFromDatabase) => {
        listPlacesIdAndFbIdFromDatabase.foreach( placeIdAndFbId =>
          saveEventsOfPlace(placeIdAndFbId._1, placeIdAndFbId._2)
        )
        Ok("Okay\n")
      }
    }
  }
}

/*
  def returnListOfIdsFromPlaces(resp : play.api.libs.ws.Response): List[String] = {
    var ids: List[String] = List()
    val responseData: JsValue = resp.json \ "data"
    val responseDataCategory_list = responseData \\ "category_list"
    var indexes: List[Int] = List()
    val listCategoryWeWantToKeep = List(JsString("179943432047564"), JsString("299714525147")) //concert venue, club

    for(j <- 0 until responseDataCategory_list.length) {
      if (listCategoryWeWantToKeep.contains((responseDataCategory_list(j) \\ "id")(0)))
        indexes = indexes :+ j
    }

    indexes.foreach{ x =>
      ids = ids :+ Json.stringify(responseData(x) \ "id").replaceAll("\"", "")
    }
    ids
  }*/

