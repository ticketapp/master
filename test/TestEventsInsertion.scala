import org.scalatestplus.play._
import models.Event
import java.util.Date

import play.api.test.FakeApplication

class TestEventsInsertion extends PlaySpec {
  "An event" must {
    val appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test"))
    "be inserted in database correctly" in {
      val event = new Event(None, None, isPublic = true, isActive = true, "event name", Option("(5.4,5.6)"),
        Option("description"), new Date(), Option(new Date()), 16, List.empty, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)
      Event.save(event) match {
        case None =>
          assert(true === false)
        case Some(eventId: Long) =>
          Event.find(eventId) mustEqual event.copy(eventId = Some(eventId))
      }
      1 mustEqual 1
    }
  }
}

