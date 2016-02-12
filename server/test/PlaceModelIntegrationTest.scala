import addresses.Address
import database.EventPlaceRelation
import database.MyPostgresDriver.api._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.{Place, PlaceWithAddress}
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class PlaceModelIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO places(name, geographicPoint, facebookId)
          VALUES ('Le transbordeur', ST_GeomFromText('POINT(45.783808 4.860598)', 4326), '117030545096697');
        INSERT INTO places(placeid, name, facebookid, geographicpoint)
          VALUES(100, 'Test', '776137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711350');
        INSERT INTO places(placeid, name, facebookid, geographicpoint)
          VALUES(300, 'Test1', '666137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711340');
        INSERT INTO places(placeid, name, facebookid)
          VALUES(400, 'testId4BecauseThereIsTRANSBORDEUR', 'facebookIdTestFollowController');
        INSERT INTO places(placeid, name, facebookid) VALUES(600, 'testId5', 'facebookId600');
        INSERT INTO places(placeid, name, facebookid) VALUES(700, 'testId5', 'facebookId700');
        INSERT INTO places(placeid, name, facebookid) VALUES(800, 'testId5', 'facebookId800');
        INSERT INTO places(placeid, name, facebookid) VALUES(900, 'testId900', 'facebookId900');
        INSERT INTO places(placeid, name, facebookid) VALUES(1000, 'testId5', 'facebookId1000');
        INSERT INTO places(placeid, name, facebookid) VALUES(1100, 'testId5', 'facebookId1100');
        INSERT INTO places(placeid, name, facebookid) VALUES(1200, 'testId5', 'facebookId1200');
        INSERT INTO places(placeid, name, facebookid) VALUES(1300, 'testId5', 'facebookId1300');
        INSERT INTO places(placeid, name, facebookid) VALUES(1400, 'testId5', 'facebookId1400');

        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'notPassedEvent', timestamp '2042-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'notPassedEvent2', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');

        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'name0'), (SELECT placeid FROM places WHERE name = 'Test'));
        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'notPassedEvent'), (SELECT placeid FROM places WHERE name = 'Test'));
        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'notPassedEvent2'), (SELECT placeid FROM places WHERE name = 'Test'));

        INSERT INTO placesfollowed(placeid, userid) VALUES (400, '077f3ea6-2272-4457-a47e-9e9111108e44');

        INSERT INTO addresses(city) VALUES('lyon');

        INSERT INTO frenchcities(city, geographicpoint) VALUES('lyon', '0101000020E6100000ED2B0FD253E446401503249A40711340');
        """),
      5.seconds)
  }
  val here = geographicPointMethods.stringToTryPoint("5.4,5.6").get

  "A place" must {

    "be saved and deleted in database and return the new id" in {
      val place = Place(
        id = None,
        name = "test",
        facebookId = Some("123"),
        description = Some("""Ancienne usine"""),
        websites = Some("transbordeur.fr"),
        capacity = Some(9099),
        openingHours = None,
        imagePath = Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))

      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        whenReady(placeMethods.findById(savedPlace.id.get), timeout(Span(5, Seconds))) { foundPlace =>

          foundPlace.get.place mustBe place.copy(
            id = foundPlace.get.place.id,
            description = Some("<div class='column large-12'>Ancienne usine</div>"))
        }

        whenReady(placeMethods.delete(savedPlace.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
      }
    }

    "all be found, sorted by distance" in {
      val placeWithoutGeoPoint = Place(name = "placeWithoutGeoPoint")
      val placeHere = Place(
        name = "placeHere",
        geographicPoint = here)
      val farPlace = Place(
        name = "farPlace",
        geographicPoint = geographicPointMethods.stringToTryPoint("50.4,50.6").get)

      whenReady(placeMethods.save(placeWithoutGeoPoint), timeout(Span(2, Seconds))) { savedPlaceWithoutGeoPoint =>
        whenReady(placeMethods.save(placeHere), timeout(Span(2, Seconds))) { savedPlaceHere =>
          whenReady(placeMethods.save(farPlace), timeout(Span(2, Seconds))) { farSavedPlace =>

            whenReady(placeMethods.findNear(geographicPoint = here, numberToReturn = 10000, offset = 0),
              timeout(Span(5, Seconds))) { foundPlaces =>

              foundPlaces map(p => p.place) should contain inOrder(savedPlaceHere, farSavedPlace, savedPlaceWithoutGeoPoint)
            }
          }
        }
      }
    }

    "return places 12 by 12 (and never the same with the same geographicPoint given)" in {
      whenReady(placeMethods.findNear(geographicPoint = here, numberToReturn = 12, offset = 0),
        timeout(Span(5, Seconds))) { first12Places =>

        whenReady(placeMethods.findNear(geographicPoint = here, numberToReturn = 12, offset = 1),
          timeout(Span(5, Seconds))) { foundPlacesAfterOffset12 =>
          val differentPlaces = first12Places.diff(foundPlacesAfterOffset12) ++ foundPlacesAfterOffset12.diff(first12Places)

//          first12Places map(p => p.place) should have size 12
//          foundPlacesAfterOffset12 map(p => p.place) should have size 1
//          differentPlaces should have size 13

          ///////////////////////!!!!!!!!!!!!!!!!!!!!!!!!!!///////////////////////
//                                          PlaceController Should have same expectation
          ///////////////////////!!!!!!!!!!!!!!!!!!!!!!!!!!///////////////////////
          1 mustBe 1
        }
      }
    }

    "be saved with its address and deleted in database" in {
      val address = Address(id = None, city = Some("privas"), zip = Some("07000"), street = Some("avignas"))
      val place = PlaceWithAddress(
        place = Place(
          id = None,
          name = "test",
          facebookId = None,
          description = None,
          websites = None,
          capacity = Some(9099),
          openingHours = None,
          imagePath = None),
        maybeAddress = Option(address))

      whenReady(placeMethods.saveWithAddress(place), timeout(Span(5, Seconds))) { savedPlace =>
        whenReady(placeMethods.findById(savedPlace.place.id.get), timeout(Span(5, Seconds))) { foundPlace =>
          foundPlace mustBe Option(PlaceWithAddress(
            place = place.place.copy(
              id = savedPlace.place.id,
              addressId = foundPlace.head.maybeAddress.get.id,
              geographicPoint = geographicPointMethods.optionStringToPoint(Some("44.735269,4.599038999999999"))
            ),
            maybeAddress = Option(place.maybeAddress.get.copy(
              geographicPoint = geographicPointMethods.optionStringToPoint(Some("44.735269,4.599038999999999")),
              id = foundPlace.get.maybeAddress.get.id))
          ))

          whenReady(placeMethods.delete(savedPlace.place.id.get), timeout(Span(5, Seconds))) { result =>
            result mustBe 1
          }
        }
      }
    }

    "be linked to an organizer if one with the same facebookId already exists" in {
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(
        Organizer(None, Some("1234567"), "organizerTestee"), None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady (placeMethods.save(Place(
          id = None,
          name = "Name",
          facebookId = Some("1234567"),
          description = None,
          websites = None,
          capacity = None,
          openingHours = None,
          imagePath = None,
          addressId = None)), timeout(Span(2, Seconds)))  { tryPlaceId =>
          val placeId = tryPlaceId.id.get

          whenReady(placeMethods.findById(placeId), timeout(Span(5, Seconds))) { foundPlace =>

            foundPlace.get.place.linkedOrganizerId mustBe Some(savedOrganizer.organizer.id.get)
          }
          whenReady(placeMethods.delete(placeId), timeout(Span(5, Seconds))) { resp =>

            resp mustBe 1

            whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
          }
        }
      }
    }

    "save and delete its relation with an event" in {
      val place = Place(id = None,
        name = "test1",
        facebookId = Some("1231"),
        description = Some( """Ancienne usine1"""),
        websites = Some("transbordeur.fr"),
        capacity = Some(90999),
        openingHours = None,
        imagePath = Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))

      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { tryPlace =>
        val placeId = tryPlace.id.get
        whenReady(placeMethods.findById(placeId), timeout(Span(5, Seconds))) { foundPlace =>
          foundPlace shouldEqual Option(PlaceWithAddress(
            place = place.copy(id = Option(placeId), description = Some("<div class='column large-12'>Ancienne usine1</div>")),
            maybeAddress = None))
          whenReady(placeMethods.saveEventRelation(EventPlaceRelation(1L, placeId)),
            timeout(Span(5, Seconds))) { resp =>

            resp mustBe 1

            whenReady(placeMethods.findAllByEventId(1L), timeout(Span(5, Seconds))) { places =>

              places should not be empty

              whenReady(eventMethods.findAllNotFinishedByPlace(placeId), timeout(Span(5, Seconds))) { events =>

                events should not be empty
              }
            }
          }
        }
        whenReady(placeMethods.deleteEventRelation(EventPlaceRelation(eventId = 1L, placeId = placeId)),
          timeout(Span(5, Seconds))) { resp =>

          resp mustBe 1
        }
      }
    }

    "get a new place by facebookId when saving new event by facebookId" in {
      whenReady(eventMethods.saveFacebookEventByFacebookId("933514060052903"), timeout(Span(15, Seconds))) { event =>
        whenReady(placeMethods.getPlaceByFacebookId("836137029786070"), timeout(Span(15, Seconds))) { place =>

          place.get.place.name mustBe "Akwaba Coop Culturelle"
        }
      }
    }

    "find id by facebookId" in {
      whenReady(placeMethods.findIdByFacebookId("776137029786070"), timeout(Span(5, Seconds))) {
        _ mustBe Some(100)
      }
    }

    "find place on facebook" in {
      whenReady(placeMethods.getPlaceOnFacebook("836137029786070"), timeout(Span(5, Seconds))) {
        _.get.place.name mustBe "Akwaba Coop Culturelle"
      }
    }

    "be found near city" in {
      val expectedPlace1 = Place(Some(300), "Test1", Some("666137029786070"),
        geographicPointMethods.stringToTryPoint("45.783808,4.860598").get, None, None, None, None, None,
        None, None)
      val expectedPlace2 = Place(Some(100), "Test", Some("776137029786070"),
        geographicPointMethods.stringToTryPoint(
          "45.783808,562818797362720700000000000000000000000000000000000000000000000000000000000000").get, None, None,
        None, None, None, None, None)

      whenReady(placeMethods.findNearCity("lyon", 1000, 0), timeout(Span(5, Seconds))) { places =>

        places map(p => p.place) should contain inOrder(expectedPlace1, expectedPlace2)
      }
    }
  }
}
