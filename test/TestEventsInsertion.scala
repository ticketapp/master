import java.util.Date

import models.Event
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.{JsValue, Json}
//import models.Genre.genresStringToGenresSet


class TestEventsInsertion extends PlaySpec with OneAppPerSuite {

  "An event" must {

    "be inserted in database correctly" in {
      models.Genre.genresStringToGenresSets(Option("jkl"))
      val event = new Event(None, None, isPublic = true, isActive = true, "event name", Option("(5.4,5.6)"),
        Option("description"), new Date(), Option(new Date()), 16, None, None, None, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)
      Event.save(event) match {
        case None =>
          assert(true === false)
        case Some(eventId: Long) =>
          Event.find(eventId) mustEqual Option(event.copy(eventId = Some(eventId)))
      }
      val a = List(1)
      val b = List(1)
      a should contain theSameElementsAs b
    }
  }
}
/*
Some(Event(Some(288),None,true,true,event name,Some((5.4,5.6)),Some(description),2015-04-28 23:29:43.486,Some(2015-04-28 23:29:43.486),16,None,None,None,List(),List(),List(),List(),List(),List())) did not equal
Event(Some(288),None,true,true,event name,Some((5.4,5.6)),Some(description),Tue Apr 28 23:29:43 CEST 2015,Some(Tue Apr 28 23:29:43 CEST 2015),16,None,None,None,List(),List(),List(),List(),List(),List()) (TestEventsInsertion.scala:24)

 */

