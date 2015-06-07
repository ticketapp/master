import org.scalatestplus.play._
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits._
import scala.language.postfixOps

class TestEventController extends PlaySpec with OneAppPerSuite  {

  "Event controller" must {

    "create an event" in {
      val eventuallyResult = route(FakeRequest(controllers.routes.EventController.createEvent()).withBody(Json.obj(
        "name" -> "name",
        "startTime" -> 5,
        "ageRestriction" -> 16,
        "startTime" -> "2015-06-30 14:00"))).get

      status(eventuallyResult) mustBe 200
    }
  }
}