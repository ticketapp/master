import attendees.{AttendeeRead, FacebookAttendee}
import database.MyPostgresDriver.api._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.libs.json._
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._

class AttendeesModelIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO events(eventid, facebookId, ispublic, isactive, name, starttime) VALUES(
          1000, 'facebookidattendeetest', true, true, 'notPassedEvent3', timestamp '2050-08-24 14:00:00');

        INSERT INTO facebookAttendees(id, attendeeFacebookId, name)
          VALUES(100, 'abcdefghij', 'name100');

        INSERT INTO facebookAttendeeEventRelations(attendeeFacebookId, eventFacebookId, attendeeStatus)
          VALUES('abcdefghij', 'facebookidattendeetest', 'D');"""),
      5.seconds)
  }
  
  "Attendee" must {

    "be saved" in {
      whenReady(attendeesMethods.save(FacebookAttendee("testSave0", "name100"))) { attendee =>
        attendee mustBe 1
      }
    }

    "be found by its facebook id" in {
      val expectedAttendee = FacebookAttendee("abcdefghij", "name100")

      whenReady(attendeesMethods.findByFacebookId("abcdefghij")) { attendee =>
        attendee mustBe Some(expectedAttendee)
      }
    }

    "all be found by an event facebook id" in {
      val expectedAttendee = FacebookAttendee("abcdefghij", "name100")

      whenReady(attendeesMethods.findAllByEventFacebookId("facebookidattendeetest")) { attendee =>
        attendee mustBe Seq(expectedAttendee)
      }
    }

    "read a json sequence of attendees" in {

      val jsonAttendees = Json.parse(
        """{"attending":{"data":[{"name":"Bli Tz","id":"165378400498201","rsvp_status":"attending"},
          |{"name":"Luis Florencio","id":"177250825964864","rsvp_status":"attending"}],
          |"paging":{"cursors":{"before":"TVRBd01ERXdOemd4TWpZAd05EYzFPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD",
          |"after":"TVRBd01EQTBNRFkzTURZAeU5UQTJPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD"},
          |"next":"https://graph.facebook.com/v2.4/866684910095368/attending?access_token=1434769156813731%257Cf2378aa93c7174712b63a24eff4cb22c&limit=25&after=TVRBd01EQTBNRFkzTURZAeU5UQTJPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD"}},"id":"866684910095368"}""".stripMargin)


      val expectedAttendees = Seq(
        AttendeeRead("Bli Tz", "165378400498201", "attending"),
        AttendeeRead("Luis Florencio", "177250825964864", "attending"))

      attendeesMethods.readJsonAttendees(jsonAttendees) mustBe expectedAttendees
    }

    "extract json attendees value from the facebook json response" in {
      val jsonAttendees =  Json.parse(
        """{"data":[{"name":"Julien Merion","id":"10153699973946072","rsvp_status":"attending"}],
          |"paging":{"cursors":{"before":"TVRBd01ERXhNREkyTmprNE16WXlPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD",
          |"after":"TlRJeU1qUXhNRGN4T2pFME5UTTBPVFkwTURBNk1UWTFNRGcwT0RrMk9EUTROVGd4"}}}""".stripMargin)

      val jsonAttendees2 = Json.parse(
        """{"attending":{"data":[{"name":"Bli Tz","id":"165378400498201","rsvp_status":"attending"},
          |{"name":"Luis Florencio","id":"177250825964864","rsvp_status":"attending"}],
          |"paging":{"cursors":{"before":"TVRBd01ERXdOemd4TWpZAd05EYzFPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD",
          |"after":"TVRBd01EQTBNRFkzTURZAeU5UQTJPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD"},
          |"next":"https://graph.facebook.com/v2.4/866684910095368/attending?access_token=1434769156813731%257Cf2378aa93c7174712b63a24eff4cb22c&limit=25&after=TVRBd01EQTBNRFkzTURZAeU5UQTJPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD"}},"id":"866684910095368"}""".stripMargin)

      attendeesMethods.extractJsonAttendeesFromFacebookJsonResponse(jsonAttendees) should not be JsNull
      attendeesMethods.extractJsonAttendeesFromFacebookJsonResponse(jsonAttendees2) should not be JsNull
    }

    "transform a read attendee to a facebook attendee" in {
      val expectedFacebookAttendee = FacebookAttendee(attendeeFacebookId = "165378400498201", name = "Bli Tz")
      attendeesMethods.attendeeReadToFacebookAttendee(
        AttendeeRead(name = "Bli Tz", id = "165378400498201", rsvp_status = "attending")) mustBe expectedFacebookAttendee
    }

    "get attendees for an event (by its facebook id)" in {
      val expectedAttendees = FacebookAttendee(attendeeFacebookId = "165378400498201", name = "Bli Tz")

      whenReady(attendeesMethods.getAllByEventFacebookId("866684910095368"), timeout(Span(5, Seconds))) { attendees =>
        attendees should contain (expectedAttendees)
      }
    }
  }
}




