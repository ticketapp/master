import addresses.Address
import database.EventPlaceRelation
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.{Place, PlaceWithAddress}
import testsHelper.GlobalApplicationForModels

import scala.language.postfixOps


class TestPlaceModel extends GlobalApplicationForModels {

  "A place" must {

    "be saved and deleted in database and return the new id" in {
      val place = Place(None, "test", Some("123"), None, Some("""Ancienne usine"""), Some("transbordeur.fr"),
        Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        whenReady(placeMethods.findById(savedPlace.id.get), timeout(Span(5, Seconds))) { foundPlace =>

          foundPlace.get.place mustBe place.copy(id = foundPlace.get.place.id,
            description = Some("<div class='column large-12'>Ancienne usine</div>"))
        }

        whenReady(placeMethods.delete(savedPlace.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
      }
    }

    "all be found, sorted by distance" in {
      val here = geographicPointMethods.stringToGeographicPoint("5.4,5.6").get

      val placeWithoutGeoPoint = Place(
        name = "placeWithoutGeoPoint",
        geographicPoint = None)
      val placeHere = Place(
        name = "placeHere",
        geographicPoint = Option(here))
      val farPlace = Place(
        name = "farPlace",
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("50.4,50.6").get))

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

    "be saved with its address and deleted in database" in {
      val address = Address(None, None, Some("privas"), Some("07000"), Some("avignas"))
      val place = PlaceWithAddress(Place(None, "test", None, None, None, None, Some(9099), None, None), address = Option(address))

      whenReady(placeMethods.saveWithAddress(place), timeout(Span(5, Seconds))) { savedPlace =>
        whenReady(placeMethods.findById(savedPlace.place.id.get), timeout(Span(5, Seconds))) { foundPlace =>
          foundPlace mustBe Option(PlaceWithAddress(
            place = place.place.copy(
              id = savedPlace.place.id,
              addressId = foundPlace.head.address.get.id,
              geographicPoint = geographicPointMethods.optionStringToOptionPoint(Some("44.735269,4.599038999999999"))
            ),
            address = Option(place.address.get.copy(
              geographicPoint = geographicPointMethods.optionStringToOptionPoint(Some("44.735269,4.599038999999999")),
              id = foundPlace.get.address.get.id))
          ))

          whenReady(placeMethods.delete(savedPlace.place.id.get), timeout(Span(5, Seconds))) { result =>
            result mustBe 1
          }
        }
      }
    }

    "be linked to an organizer if one with the same facebookId already exists" in {
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(Organizer(None, Some("1234567"), "organizerTestee"),
        None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady (placeMethods.save(Place(None, "Name", Some("1234567"), None, None, None, None, None, None, None)),
          timeout(Span(2, Seconds)))  { tryPlaceId =>
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
      val place = Place(None, "test1", Some("1231"), None, Some("""Ancienne usine1"""),
        Some("transbordeur.fr"), Some(90999), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))

      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { tryPlace =>
        val placeId = tryPlace.id.get
        whenReady(placeMethods.findById(placeId), timeout(Span(5, Seconds))) { foundPlace =>
          foundPlace shouldEqual Option(PlaceWithAddress(
            place = place.copy(id = Option(placeId), description = Some("<div class='column large-12'>Ancienne usine1</div>")),
            address = None))
          whenReady(placeMethods.saveEventRelation(EventPlaceRelation(1L, placeId)),
            timeout(Span(5, Seconds))) { resp =>

            resp mustBe 1

            whenReady(placeMethods.findAllByEventId(1L), timeout(Span(5, Seconds))) { places =>

              places should not be empty

              whenReady(eventMethods.findAllByPlace(placeId), timeout(Span(5, Seconds))) { events =>

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
        Some(geographicPointMethods.stringToGeographicPoint("45.783808,4.860598").get), None, None, None, None, None,
        None, None)
      val expectedPlace2 = Place(Some(100), "Test", Some("776137029786070"),
        Some(geographicPointMethods.stringToGeographicPoint(
          "45.783808,562818797362720700000000000000000000000000000000000000000000000000000000000000").get), None, None,
        None, None, None, None, None)

      whenReady(placeMethods.findNearCity("lyon", 10, 0), timeout(Span(5, Seconds))) { places =>

        places map(p => p.place) should contain inOrder(expectedPlace1, expectedPlace2)
      }
    }
  }
}
