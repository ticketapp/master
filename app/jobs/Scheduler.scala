package jobs

import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits._

object Scheduler {
  def start = {
    /*WS.url("http://codebutler.com/firesheep/").get().map { response =>
      println(response.body)
    }*/
    /*var array, places, events, searchPlaces = Array()
    var placesName = Array("Le Transbordeur", "ninkasi")
    var i = 0
    for(i <- 1 to placesName.length){
      WS.url("https://graph.facebook.com/v2.2/search?q=" + placesName(i) + "&limit=200&type=page&access_token=CAACEdEose0cBAJxDMsfYWRDVKIeEB34ZCvHerWX1eUsuc0iahpVgVVbuVZCLHXpZAPZCx5HNpDdA7pxZAsNWLPzpjP8IFUjARVGRZCdoZAqtpUGAas3XQgpoo4wFLVl2UYMfbslZA0esZBvAktIwSgb0PgWkiV8l7AkLK975nHiXDFQBmbZAi3KDHpR66zuPkGnvHeYLrySmad9B9Kr0VRoU1JuGIKI6RMtAsZD")
        .get().map { response =>
          println(response)
      }
    }*/

  }
}
/*
function searchPl (i) {
$http.get('https://graph.facebook.com/v2.2/search?q=' + placesName[i] + '&limit=200&type=page&access_token=CAACEdEose0cBAJxDMsfYWRDVKIeEB34ZCvHerWX1eUsuc0iahpVgVVbuVZCLHXpZAPZCx5HNpDdA7pxZAsNWLPzpjP8IFUjARVGRZCdoZAqtpUGAas3XQgpoo4wFLVl2UYMfbslZA0esZBvAktIwSgb0PgWkiV8l7AkLK975nHiXDFQBmbZAi3KDHpR66zuPkGnvHeYLrySmad9B9Kr0VRoU1JuGIKI6RMtAsZD').
success(function (data, status, headers, config) {
data = data.data;
for (var iv = 0; iv < data.length; iv++) {
if (data[iv].category == 'Concert venue' || data[iv].category == 'Club') {
searchPlaces.push(data[iv]);
} else if (data[iv].category_list != undefined) {
for (ii = 0; ii < data[iv].category_list.length; ii++) {
if (data[iv].category_list[ii].name == 'Concert Venue' || data[iv].category_list[ii].name == 'Club') {
searchPlaces.push(data[iv]);
}
}
}
}
if (i == placesName.length -1) {
detailPlaces(searchPlaces)
}
}).
error(function (data, status, headers, config) {
// called asynchronously if an error occurs
// or server returns response with an error status.
});
}
function detailPlaces (searchPlaces) {
for (var j = 0; j < searchPlaces.length; j++) {
searchAllPlaces(j, searchPlaces)
}
}
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