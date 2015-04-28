import models.Genre
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.{JsValue, Json}
//import models.Genre.genresStringToGenresSet

import play.api.test.FakeApplication

class TestEventsInsertion extends PlaySpec {

  "An event" must {

    "be inserted in database correctly" in new App() {
      models.Genre.genresStringToGenresSets(Option("jkl"))
    /*  val event = new Event(None, None, isPublic = true, isActive = true, "event name", Option("(5.4,5.6)"),
        Option("description"), new Date(), Option(new Date()), 16, None, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)
      Event.save(event) match {
        case None =>
          assert(true === false)
        case Some(eventId: Long) =>
          Event.find(eventId) mustEqual event.copy(eventId = Some(eventId))
      }*/
      val a = List(1)
      val b = List(2)
      a should contain b
    }
  }
}

