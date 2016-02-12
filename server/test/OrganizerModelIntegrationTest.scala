import addresses.Address
import database.EventOrganizerRelation
import database.MyPostgresDriver.api._
import eventsDomain.{Event, EventWithRelations}
import org.joda.time.DateTime
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.Place
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class OrganizerModelIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO organizers(name) VALUES('name0');
        INSERT INTO organizers(organizerid, name, facebookid, geographicpoint)
          VALUES(100, 'name1', 'facebookId', '0101000020E6100000ED2B0FD253E446401503249A40711350');
        INSERT INTO organizers(organizerid, name, facebookid, geographicpoint)
          VALUES(300, 'name2', 'facebookId1', '0101000020E6100000ED2B0FD253E446401503249A40711340');

        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
        INSERT INTO events(eventId, ispublic, isactive, name, starttime, endtime)
          VALUES(100, true, true, 'notPassedEvent2', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');

        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'name0'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'eventPassed'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'notPassedEvent2'), (SELECT organizerid FROM organizers WHERE name = 'name0'));

        INSERT INTO organizersfollowed(organizerid, userid)
          VALUES((SELECT organizerId FROM organizers WHERE name = 'name0'), '077f3ea6-2272-4457-a47e-9e9111108e44');

        INSERT INTO frenchcities(city, geographicpoint) VALUES('lyon', '0101000020E6100000ED2B0FD253E446401503249A40711340');
        """),
      5.seconds)
  }

  "An Organizer" must {

    "be saved and deleted in database" in {
      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = geographicPointMethods.stringToTryPoint("5.4,5.6").get)
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)),
        timeout(Span(5, Seconds))) { savedOrganizer =>
        savedOrganizer mustEqual OrganizerWithAddress(
          organizer = organizer.copy(
            id = Some(savedOrganizer.organizer.id.get),
            description = Some("<div class='column large-12'>description</div>")),
          address = None)
        whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {
          _ mustBe 1
        }
      }
    }

    "find organizers" in {
      val expectedOrganizerNames = Seq("name0", "name1", "name2")
      whenReady(organizerMethods.findSinceOffset(0, 10000), timeout(Span(5, Seconds))) { foundOrganizers =>

        foundOrganizers map (_.organizer.name) should contain theSameElementsAs expectedOrganizerNames
      }
    }

    "find organizer by facebookId" in {
      whenReady(organizerMethods.findIdByFacebookId(Option("facebookId")), timeout(Span(5, Seconds))) { foundOrganizerId =>
        foundOrganizerId mustBe Some(100)
      }
    }

    "find organizers near geoPoint" in {
      val organizer = Organizer(None, Option("facebookId2222"), "organizerTest2222", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = geographicPointMethods.stringToTryPoint("150,56").get)
      val organizer1 = Organizer(None, Option("facebookId22222"), "organizerTest22222", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = geographicPointMethods.stringToTryPoint("6.4,6.6").get)
      val organizer2 = Organizer(None, Option("facebookId222222"), "organizerTest222222", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = geographicPointMethods.stringToTryPoint("7.4,7.6").get)

      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)),
        timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer1, None)),
          timeout(Span(5, Seconds))) { savedOrganizer1 =>
          whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer2, None)),
            timeout(Span(5, Seconds))) { savedOrganizer2 =>
            whenReady(organizerMethods.findNear(geographicPointMethods.stringToTryPoint("6.4,6.6").get, 10, 0),
              timeout(Span(5, Seconds))) { foundOrganizers =>

              foundOrganizers should contain inOrder(savedOrganizer1, savedOrganizer2, savedOrganizer)
            }
          }
        }
      }
    }

    "be found near city" in {
      whenReady (organizerMethods.findNearCity("lyon", 10, 0),  timeout(Span(5,  Seconds))) { response =>
        val organizer = OrganizerWithAddress(
          organizer = Organizer(Some(300), Some("facebookId1"), "name2", None, None, None, None, None, verified = false,
            None, geographicPointMethods.stringToTryPoint("45.783808, 4.860598").get, None),
          address = None)
        val organizer2 = OrganizerWithAddress(
          organizer = Organizer(Some(100), Some("facebookId"), "name1", None, None, None, None, None, verified = false,
            None, geographicPointMethods.stringToTryPoint(
              "45.783808,562818797362720700000000000000000000000000000000000000000000000000000000000000").get, None),
          address = None)

        response must contain inOrder(organizer, organizer2)
      }
    }

    "not be saved twice and return the organizerId" in {
      val organizer = Organizer(None, Option("facebookId3"), "organizerTest3", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = geographicPointMethods.stringToTryPoint("5.4,5.6").get)
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)),
        timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) {

          _ mustBe savedOrganizer
        }
        whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {

          _ mustBe 1
        }
      }
    }

    "be linked to a place if one with the same facebookId already exists" in {
      whenReady (placeMethods.save(Place(
        id = None,
        name = "Name1",
        facebookId = Some("1234567891"),
        description = None,
        websites = None,
        capacity = None,
        openingHours = None,
        imagePath = None,
        addressId = None)), timeout(Span(2, Seconds)))  { tryPlace =>
        val placeId = tryPlace.id.get
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(
          organizer = Organizer(None, Some("1234567891"), "organizerTest2"),
          address = None)
        ), timeout(Span(5, Seconds))) { savedOrganizer =>
          whenReady(organizerMethods.findById(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {
            case Some(organizerWithAddress: OrganizerWithAddress) =>

              organizerWithAddress.organizer.linkedPlaceId mustBe Some(placeId)

            case _ =>
              throw new Exception("TestOrganizerModel.musBeLinkedToAPlace: error on save or find")
          }
        }
      }
    }

    "save an organizer with its event relation" in {
      val geoPoint = geographicPointMethods.stringToTryPoint("5.4,5.6").get
      val organizer = OrganizerWithAddress(Organizer(None, None, "organizerTest2", geographicPoint = geoPoint))

      val event = Event(None, None, isPublic = true, isActive = true, "name", geoPoint, None, new DateTime(), None, 16,
        None, None, None)

      whenReady(eventMethods.save(EventWithRelations(event)), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(organizerMethods.saveWithEventRelation(organizer, savedEvent.id.get),
          timeout(Span(5, Seconds))) { savedOrganizer =>
          whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { foundEvent =>
            whenReady(organizerMethods.findAllByEventId(foundEvent.get.event.id.get),
              timeout(Span(5, Seconds))) { foundOrganizers =>

              foundOrganizers.head mustBe savedOrganizer
              foundOrganizers.size mustBe 1
            }
          }
        }
      }
    }

    "get the info about the organizer on Facebook" in {
      whenReady (organizerMethods.getOrganizerInfo(Option("164354640267171")), timeout(Span(5, Seconds))) { organizerInfos =>

        organizerInfos.get.organizer.name mustBe "Le Transbordeur"
      }
    }

    "save and find an organizer with his address" in {
      whenReady (organizerMethods.getOrganizerInfo(Option("164354640267171")), timeout(Span(5, Seconds))) { organizerInfos =>
        whenReady(organizerMethods.saveWithAddress(organizerInfos.get), timeout(Span(5, Seconds))) { savedOrganizer =>


        organizerInfos.get.organizer.name mustBe "Le Transbordeur"
          savedOrganizer.organizer.name mustBe "Le Transbordeur"
          savedOrganizer.address.get mustBe Address(savedOrganizer.address.get.id,
            geographicPointMethods.stringToTryPoint("45.7839103,4.860398399999999").get,
          Some("villeurbanne"),Some("69100"),Some("3 boulevard de la bataille de stalingrad"))
        }
      }
    }

    "find organizers containing" in {
      whenReady(organizerMethods.findAllContaining("name"), timeout(Span(5, Seconds))) {
        _.size mustBe 3
      }
    }

    "save events relations" in {
      whenReady(organizerMethods.saveEventRelations(Seq(EventOrganizerRelation(100, 300))), timeout(Span(5, Seconds))) { response =>

        response mustBe true
      }
    }
  }
}

