import attendees.{AttendeeRead, FacebookAttendee}
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.specs2.control.Ok
import play.api.libs.json._
import testsHelper.GlobalApplicationForModels


class TestAttendeesModel extends GlobalApplicationForModels {

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
      whenReady(attendeesMethods.getAllByEventFacebookId("866684910095368"), timeout(Span(5, Seconds))) { attendees =>
        attendees.statusText must equal("OK")
      }
    }

    "get attendees for a next path" in {
      val expectedAttendees = FacebookAttendee(attendeeFacebookId = "10153907895484801",name = "Romain Paranoise Bfd")
      val path = "https://graph.facebook.com/v2.4/866684910095368/attending?access_token=1434769156813731%257Cf2378aa93c7174712b63a24eff4cb22c&limit=100&after=T0RBeE1qTXpNVGt6T2pFME5UTTBPVFkwTURBNk1UWTFNRGcwT0RrMk9EUTROVGd4"

      whenReady(attendeesMethods.getAttendeesRecursively(path), timeout(Span(5, Seconds))) { attendees =>
        attendees should contain (expectedAttendees)
      }
    }

    "Transform a facebook response to a seq of attendees" in {
      val expectedAttendees = FacebookAttendee(attendeeFacebookId = "165378400498201",name = "Bli Tz")
      val facebookResponse = Json.parse("""{"data":[
            {"name":"Bli Tz",
            "id":"165378400498201",
            "rsvp_status":"attending"
            },
            {"name":"Yohann Cvzl",
            "id":"175122322847900",
            "rsvp_status":"attending"
            }
          ]}""")

      val attendees = attendeesMethods.facebookResponseToSeqAttendees(facebookResponse)

      attendees should contain (expectedAttendees)
    }

    "get maybe next attendees" in {
      val expectedAttendees = Seq(FacebookAttendee(attendeeFacebookId = "165378400498201",name = "Bli Tz"))
      val facebookResponse = Json.parse("""{
          "data":[
            {"name":"Bli Tz",
            "id":"165378400498201",
            "rsvp_status":"attending"
            },
            {"name":"Yohann Cvzl",
            "id":"175122322847900",
            "rsvp_status":"attending"
            }
          ],
          "paging":{
            "cursors":{
              "before":"TVRBd01ERXdOemd4TWpZAd05EYzFPakUwTlRNME9UWTBNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD",
              "after":"T0RBeE1qTXpNVGt6T2pFME5UTTBPVFkwTURBNk1UWTFNRGcwT0RrMk9EUTROVGd4"
            },
            "next":"https://graph.facebook.com/v2.4/866684910095368/attending?access_token=1434769156813731%257Cf2378aa93c7174712b63a24eff4cb22c&limit=100&after=T0RBeE1qTXpNVGt6T2pFME5UTTBPVFkwTURBNk1UWTFNRGcwT0RrMk9EUTROVGd4"
          }
        }""")

      whenReady(attendeesMethods.getMaybeNextAttendees(facebookResponse, expectedAttendees),  timeout(Span(5, Seconds))) { attendees =>
        attendees should contain (expectedAttendees.head)
      }
    }
  }
}




