import java.util.Date
import controllers.DAOException
import models.Artist
import models.Artist._
import models.Event
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.Json
import play.api.test.{WithApplication, PlaySpecification, FakeRequest}
import securesocial.core.Identity

import play.api.Play.current

import scala.util.Success
import scala.util.Failure
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test._
import play.api.test.Helpers._
import json.JsonHelper._

class TestTrackController extends PlaySpec with OneAppPerSuite {

  "TrackController" must {

    "upsert rating up for a user" in {
      val eventuallyResult = route(FakeRequest(controllers.routes.TrackController.upsertRatingForUser())
        .withBody(Json.obj(
        "trackId" -> "eada2738-445b-4257-9c60-da955e2c7da9",
        "rating" -> 1000,
        "reason" -> "r"))
        .withFormUrlEncodedBody(("username", "test@example.com"), ("password", "MyTestPassword"))).get


//      status(eventuallyResult) mustBe 200
    }

    "upsert rating down for a user" in {
//      upsertRatingForUser
    }

    "upsert rating down and add a reason for a user" in {
//      upsertRatingForUser
    }
  }
}

