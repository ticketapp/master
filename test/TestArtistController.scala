import json.JsonHelper._
import models.Event
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class TestArtistController extends PlaySpec with OneAppPerSuite {

  "ArtistController" must {

    /*"be followed by a user" in {
      val eventuallyResult = controllers.EventController.findNearCity("abc", 1, 0)(FakeRequest())
      status(eventuallyResult) mustBe 200
      contentAsJson(eventuallyResult) mustBe Json.toJson(Seq.empty[Event])
    }*/
  }
}
