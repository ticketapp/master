package controllers

import java.io.{IOException, FileNotFoundException}
import java.util.Date
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.Event
import scala.io.Source
import play.api.mvc.Results._

object Test extends Controller{
  var token = "CAAJZCFQiqd2cBALgIhwnVBjNibfgRctH9BOZCdORb7q9CV8ZCq3BwjWZBLTeSo0UaldCJOJLDgn0LhD672XKWVIpAZCWxPWwZBET1V0E2facbMt9tGx7LpCdT5V4zO9cbqLHmcDHXWNmmrOe4lMRk0FvAwlZCl5LHXtENa3D157k0GxIIn8TK2G2Ag48ZAMnZBhhnIYKm8hfIKqgyPBr9U7ZBjzv16ZA6WzmZC8ZD"

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
  }

  def returnListOfIdsFromEvents(resp : Response): List[String] = {
    var ids: List[String] = List()
    val responseDataIds = resp.json \ "data" \\ "id"
    for(j <- responseDataIds) {
      ids = ids :+ Json.stringify(j).replaceAll("\"", "")
    }
    ids
  }

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

  def saveEvent(eventDescription: String, eventResp: Response) = {
    val eventJson = eventResp.json
    println(eventJson)

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

   /*placeId: Long,
    name: String,
    addressID: Option[Long] = None,
    facebookId: Option[String] = None,
    facebookImage: Option[String] = None,
    description: Option[String] = None,
    webSite: Option[String] = None,
    facebookMiniature: Option[String] = None,
    capacity: Option[String] = None,
    openingHours: Option[String] = None*/

    //val place = new( Place(-1L,  ))

    //val event =
    //println(event)
    Event.save(new Event(-1L, facebookId, true, true, new Date, name, None, None,
      eventDescription, startTime, endTime, 16, List(), List(), List(), List(), List()))
  }


  /*def saveEventsOfPlace(placeName: String) = {
    for {
      idsOfPlace <- WS.url("https://graph.facebook.com/v2.2/search?q=" + placeName.replace(" ", "+") +
        "&limit=200&type=page&access_token=" + token).get

      placesIds: List[String] = returnListOfIdsFromPlaces(idsOfPlace)

      listOfEventsByPlacesId <- Future.sequence(placesIds.map( placeId =>
        WS.url("https://graph.facebook.com/v2.2/" + placeId + "/events/?access_token=" + token).get) )
    } yield {
      listOfEventsByPlacesId.map( event => { println(event)
        returnListOfIdsFromEvents(event).map( eventId =>
          WS.url("https://graph.facebook.com/v2.2/" + eventId +
            "?fields=cover,description,name,start_time,end_time,owner" + "&access_token=" + token
          ).get.map( response =>
            saveEvent(addBannerToEventDescription(Json.stringify(response.json \ "description"),
              Json.stringify(response.json \ "name"), Json.stringify(response.json \ "cover" \ "source")),
              response) ) ) } )
    }
  }*/

  def saveEventsOfPlace(placeName: String) = {
    for {
      idsOfPlace <- WS.url("https://graph.facebook.com/v2.2/search?q=" + placeName.replace(" ", "+") +
        "&limit=200&type=page&access_token=" + token).get

      placesIds: List[String] = returnListOfIdsFromPlaces(idsOfPlace)
      listOfEventsByPlacesId <- Future.sequence(placesIds.map(placeId =>
        WS.url("https://graph.facebook.com/v2.2/" + placeId + "/events/?access_token=" + token).get) )

    } yield {
      listOfEventsByPlacesId.map(event => {
        println(event)
        returnListOfIdsFromEvents(event).map(eventId =>
          WS.url("https://graph.facebook.com/v2.2/" + eventId +
            "?fields=cover,description,name,start_time,end_time,owner" + "&access_token=" + token
          ).get.map(response =>
            saveEvent(addBannerToEventDescription(Json.stringify(response.json \ "description"),
              Json.stringify(response.json \ "name"), Json.stringify(response.json \ "cover" \ "source")),
              response)))
      })
    }
  }

  def test1 = Action {
    //val placesName = List("Le Transbordeur", "ninkasi")
    //for (placeName <- placesName) saveEventsOfPlace(placeName)
    try {
      val fileLines = Source.fromFile("textFiles/400to1200PlacesStructures.txt").getLines.toList
      var res = 0
      for (i <- 0 until fileLines.length ) {
        if (fileLines(i) == "Salles de 400 à 1200 places") {
          res += 1
          saveEventsOfPlace(fileLines(i - 1).replace(" ", "+"))
        }
      }
      println(res)
    } catch {
      case e: FileNotFoundException => println("Couldn't find that file.")
      case e: IOException => println("Had an IOException trying to read that file")
    }

    Ok("qsdqsdqs")
  }
}
