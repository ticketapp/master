package controllers

import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.concurrent.{Await, Future}
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration.Duration


object Teeest extends Controller{
  def returnListOfIdsFromPlaces(resp : play.api.libs.ws.Response): List[String] = {
    var ids: List[String] = List()
    val responseData: JsValue = (resp.json \ "data")
    val responseDataCategory_list = (responseData \\ "category_list" )
    var indexes: List[Int] = List()

    for(j <- 0 until responseDataCategory_list.length)
      if((responseDataCategory_list(j) \\ "id")(0) == JsString("179943432047564"))
        indexes = indexes :+ j

    indexes.foreach{ x =>
      ids = ids :+ Json.stringify((responseData(x) \ "id")).replaceAll("\"", "")
    }
    ids
  }

  def returnListOfIdsFromEvents(resp : play.api.libs.ws.Response): List[String] = {
    var ids: List[String] = List()
    val responseData: JsValue = (resp.json \ "data")
    val responseDataIds = (responseData \\ "id" )
    for(j <- responseDataIds) {
      ids = ids :+ Json.stringify(j).replaceAll("\"", "")
    }
    ids
  }

  def returnIdsOfPlace(placeName: String) = {
    for {
      idsOfPlace <- WS.url("https://graph.facebook.com/v2.2/search?q=" + placeName.replace(" ", "+") + "&limit=200&type=page&access_token=" +
        "CAACEdEose0cBAHre0KRxYzlneiB63r16ZB671SlSczvdIhqYhB5AwuWZAkPynSOrbhcMogDZCiOU39Y9WRuvqpZBgQ7kRv7E7rRkEfQ1Cq7GyAZA2mDbKn5uuD5ZARvLhZBErHMMGYqIRTZBNqzhWW7UH8bZCMV352lj12XZBxvwL6DuMFoAV17i5kZAswQTHanZBrdC5OjAgo4GGFVRUkAgdHsOftkZAyIFseQoZD")
        .get
      placesIds: List[String] = returnListOfIdsFromPlaces(idsOfPlace)

      /*listOfMusicPlaces <- Future.sequence(placesIds.map(placeId =>
        WS.url("https://graph.facebook.com/v2.2/" + placeId + "/?access_token=" +
          "CAACEdEose0cBAAdSr4yRU34JjbuJvXXn4GSm2mRBcfJqXOo6VHYIWq8R7ngjUepTKgkNeoO4doHZB7XhX4dMynqZCWhwZAM4bZAHN499msoWpJA125FZBZAkRdXAw1MZBo1TtzCF7KjsOAXVxM1C2uQSZAfS4cIXgaNRbbyA7wqqRZCBQVZBZB7yiNTinbmFiEoZAzoZCtjoJ7wHZCWD8lVUIzqEYMuwLnnshju1UZD")
          .get))*/

      listOfEventsByPlacesId <- Future.sequence(placesIds.map(placeId =>
        WS.url("https://graph.facebook.com/v2.2/" + placeId + "/events/?access_token=" +
          "CAACEdEose0cBAHre0KRxYzlneiB63r16ZB671SlSczvdIhqYhB5AwuWZAkPynSOrbhcMogDZCiOU39Y9WRuvqpZBgQ7kRv7E7rRkEfQ1Cq7GyAZA2mDbKn5uuD5ZARvLhZBErHMMGYqIRTZBNqzhWW7UH8bZCMV352lj12XZBxvwL6DuMFoAV17i5kZAswQTHanZBrdC5OjAgo4GGFVRUkAgdHsOftkZAyIFseQoZD")
          .get))
    } yield {
      //println(respA.json)
      //listOfMusicPlaces.map(response => println(response.json))

      listOfEventsByPlacesId.map(event => println(event.json))

      /*
      //eventsIds: List[String] = returnListOfIdsFromEvents(listOfEventsByPlacesId)

      listofEventsDescription <- Future.sequence(listOfEventsByPlacesId.map(eventId =>
        WS.url("https://graph.facebook.com/v2.2/" + eventId + "/?fields=cover&access_token=" +
          "CAACEdEose0cBAHre0KRxYzlneiB63r16ZB671SlSczvdIhqYhB5AwuWZAkPynSOrbhcMogDZCiOU39Y9WRuvqpZBgQ7kRv7E7rRkEfQ1Cq7GyAZA2mDbKn5uuD5ZARvLhZBErHMMGYqIRTZBNqzhWW7UH8bZCMV352lj12XZBxvwL6DuMFoAV17i5kZAswQTHanZBrdC5OjAgo4GGFVRUkAgdHsOftkZAyIFseQoZD")
          .get))
       */
/*
      listOfEventsByPlacesId.map(response => returnListOfIdsFromEvents(response))

      listofEventsDescription.map(response => response.json)
*/
    }
  }

  def teest = Action {

    val placesName = List("Le Transbordeur", "ninkasi")


    for (a <- placesName) returnIdsOfPlace(a)


    Ok("qsdqsdqs")
  }
}
/*
function searchAllPlaces (j, searchPlaces) {
$http.get('https://graph.facebook.com/v2.2/'+ searchPlaces[j].id +'/?access_token=CAACEdEose0cBABpIOqNdnIf6rP69y5atZC0MkYEpcGSVacO8zBPtc9LdKyUczoqkwvoh4TSTnt3M2vaL24CZADIAsUZBpQdE5o5dLBgGmaHjp1NJg1DrHHKrk7TBd0JkarAFsgKcmYoKRI6tdcGYjkjg1t07jlb37TLlbxvDMntWmWwFIRT3LvTZAIhsTQ8JxWFCkSspAomoMWh0OZBYZCqZBaQRaBhXRkZD').
success(function(data, status, headers, config) {
flag = 0;
for (m = 0; m < places.length; m++) {
if (places[m].id == data.id){
flag = 1;
} else if (places[m].location.latitude == data.location.latitude && places[m].location.longitude == data.location.longitude && places[m].likes > data.likes) {
flag = 1;
} else if (places[m].location.latitude == data.location.latitude && places[m].location.longitude == data.location.longitude && places[m].likes < data.likes) {
places.splice(m, 1);
}
}
if (data.location.country != 'France') {
flag = 1;
}
if (flag == 0){
places.push(data);
}
if (j == searchPlaces.length -1) {
searchEvent(places);
}
}).
error(function(data, status, headers, config) {
// called asynchronously if an error occurs
// or server returns response with an error status.
});
}
function searchEvent(places) {
for (var k=0; k < places.length; k++) {
getEvents(k, places)
pushPlaces(places[k])
}
}
function getEvents (k, places) {
$http.get('https://graph.facebook.com/v2.2/'+ places[k].id +'/events/?access_token=CAACEdEose0cBABpIOqNdnIf6rP69y5atZC0MkYEpcGSVacO8zBPtc9LdKyUczoqkwvoh4TSTnt3M2vaL24CZADIAsUZBpQdE5o5dLBgGmaHjp1NJg1DrHHKrk7TBd0JkarAFsgKcmYoKRI6tdcGYjkjg1t07jlb37TLlbxvDMntWmWwFIRT3LvTZAIhsTQ8JxWFCkSspAomoMWh0OZBYZCqZBaQRaBhXRkZD').
success(function(data, status, headers, config) {
getEvent(data.data);
}).
error(function(data, status, headers, config) {
// called asynchronously if an error occurs
// or server returns response with an error status.
});
}
function getEvent (data) {
for (var m = 0; m < data.length; m++) {
getEventById(data[m])
}
}
function getEventById (eventToGet) {
$http.get('https://graph.facebook.com/v2.2/'+ eventToGet.id +'/?access_token=CAACEdEose0cBABpIOqNdnIf6rP69y5atZC0MkYEpcGSVacO8zBPtc9LdKyUczoqkwvoh4TSTnt3M2vaL24CZADIAsUZBpQdE5o5dLBgGmaHjp1NJg1DrHHKrk7TBd0JkarAFsgKcmYoKRI6tdcGYjkjg1t07jlb37TLlbxvDMntWmWwFIRT3LvTZAIhsTQ8JxWFCkSspAomoMWh0OZBYZCqZBaQRaBhXRkZD').
success(function(data, status, headers, config) {
var event = data;
var links = /((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)/gi;
event.description = event.description.replace(/(\n\n)/g, " <br/><br/></div><div class='column large-12'>");
event.description = event.description.replace(/(\n)/g, " <br/>");
if (matchedLinks = event.description.match(links)) {
var ma = matchedLinks;
var unique = [];
for (var ii = 0; ii < ma.length; ii++) {
var current = ma[ii];
if (unique.indexOf(current) < 0) unique.push(current);
}
for (var i=0; i < unique.length; i++) {
event.description = event.description.replace(new RegExp(unique[i],"g"),
"<a href='" + unique[i]+ "'>" + unique[i] + "</a>")
}
}
getCover(event)
}).
error(function(data, status, headers, config) {
// called asynchronously if an error occurs
// or server returns response with an error status.
});
}
function getCover (event) {
$http.get('https://graph.facebook.com/v2.2/'+ event.id +'/?fields=cover&access_token=CAACEdEose0cBABpIOqNdnIf6rP69y5atZC0MkYEpcGSVacO8zBPtc9LdKyUczoqkwvoh4TSTnt3M2vaL24CZADIAsUZBpQdE5o5dLBgGmaHjp1NJg1DrHHKrk7TBd0JkarAFsgKcmYoKRI6tdcGYjkjg1t07jlb37TLlbxvDMntWmWwFIRT3LvTZAIhsTQ8JxWFCkSspAomoMWh0OZBYZCqZBaQRaBhXRkZD').
success(function(data, status, headers, config) {
event.description = '<img class="width100p" src="' + data.cover.source + '"/>' + '<div class="columns large-12"><h2>' + event.name + '</h2></div>' + '<div class="columns large-12">' +  event.description + '</div>';
event.image = data.cover.source;
pushEvents (event)
}).
error(function(data, status, headers, config) {
event.description = '<div class="row">' + event.description + '</div>';
pushEvents (event);
// called asynchronously if an error occurs
// or server returns response with an error status.
});
}
function pushPlaces (place) {
//save here places
}
function pushEvents (event) {
//save here events
console.log(event)
}*/