import java.util.Date
import controllers.DAOException
import models.{Place, Artist, Event}
import models.Artist._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.Json
import play.api.test.{WithApplication, PlaySpecification, FakeRequest}
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.util.Success
import scala.util.Failure
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test._
import play.api.test.Helpers._
import json.JsonHelper._

class TestPlaceController extends PlaySpec with OneAppPerSuite {

  "PlaceController" must {
/*
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
    }
  }
}
