package jobs

import java.util.Date
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Place, Event, Image}
import scala.util.{Failure, Success}

/*




FAIRE DES UPDATES SI LEVENT EXISTE DEJA



 */
object Scheduler {
  val token = play.Play.application.configuration.getString("facebook.token")

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

  def saveEvent(eventDescription: String, eventResp: Response, placeId: Long, imgPath: String) = {
    val eventJson = eventResp.json

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
    Image.save(new Image(-1L, imgPath.replaceAll("\"", ""), Some(eventId), None))
  }

  def returnListOfIdsFromEvents(resp : Response): List[String] = {
    var ids: List[String] = List()
    val responseDataIds = resp.json \ "data" \\ "id"
    for(j <- responseDataIds) {
      ids = ids :+ Json.stringify(j).replaceAll("\"", "")
    }
    ids
  }
/*

 val eventFuture = WS.url("https://graph.facebook.com/v2.2/ez.dubstep.night&access_token=" + token).get
    eventFuture onComplete {
      case Success(posts) => println(posts)
      case Failure(t) => println("An error has occured: " + t.getMessage)
    }
    println("avant ??")
 */
  def saveEventsOfPlace(placeId: Long, placeFacebookId: String) = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/?access_token=" +
      token).get onComplete {
      case Success(events) => returnListOfIdsFromEvents(events).map( eventId =>
        WS.url("https://graph.facebook.com/v2.2/" + eventId +
          "?fields=cover,description,name,start_time,end_time,owner" + "&access_token=" + token)
          .get onComplete {
          case Success(eventDetailed) =>  val description = Json.stringify(eventDetailed.json \ "description")
            val name = Json.stringify(eventDetailed.json \ "name")
            val imgPath = Json.stringify(eventDetailed.json \ "cover" \ "source")
            saveEvent(addBannerToEventDescription(description, name, imgPath), eventDetailed, placeId, imgPath)

          case Failure(f) => println("An error has occurred in saveEventsOfPlace: " + f.getMessage)
        } )
      case Failure(f) => println("An error has occurred in saveEventsOfPlace: " + f.getMessage)
    }
  }

  def start = {
    Place.findAllIdsAndFacebookIds match {
      case Failure(f) => println("Erreur dans le scheduler :\n" + f + "\n")
      case Success(listPlacesIdAndFbIdFromDatabase) => {
        listPlacesIdAndFbIdFromDatabase.foreach( placeIdAndFbId =>
          saveEventsOfPlace(placeIdAndFbId._1, placeIdAndFbId._2)
        )
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
