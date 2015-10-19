/*
import models.Place
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class TestPlaceController extends PlaySpec with OneAppPerSuite {

  "PlaceController" must {
/*/*
city: "Châteauneuf-de-Gadagne"
street: "500 chemin des matouses"
zip: "84470"
capacity: 1786
description: undefined
facebookId: "836137029786070"
imagePath: "https://scontent.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/s720x720/11268366_887610161305423_50446740637...
t;
name: "Akwaba Coop Culturelle"
webSite: "http://akwaba.coop";
 */
    "save a place and delete it" in {

      val eventuallyResult = route(FakeRequest(controllers.routes.PlaceController.create())
        .withBody(Json.obj("name" -> "bob"))).get

      val place = Place(None, "bob", None, None, None, None, None, None, None, None)

      status(eventuallyResult) mustBe 200
      /*
      println(contentAsJson(eventuallyResult))
      contentAsJson(eventuallyResult) mustBe Json.toJson(place)*/
    }

    "save a place with its address and delete it" in {

      val eventuallyResult = route(FakeRequest(controllers.routes.PlaceController.create())
        .withBody(Json.obj(
          "name" -> "bob",
          "street" -> "500 chemin des matouses",
          "zip" -> "84470",
          "city" -> "Châteauneuf-de-Gadagne"))).get

      val place = Place(None, "bob", None, None, None, None, None, None, None, None)

      status(eventuallyResult) mustBe 200
    /*  println(contentAsJson(eventuallyResult))
//      eventuallyResult should not be empty
      contentAsJson(eventuallyResult) mustBe Json.toJson(place)*/
    }*/
  }
}
*/
