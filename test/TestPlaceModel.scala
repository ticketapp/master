import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestPlaceModel extends GlobalApplicationForModels {

  "A place" must {

    "be saved and deleted in database and return the new id" in {
      val place = Place(None, "test", Some("123"), None,
        Some("""Ancienne usine"""),
        Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        try {
          whenReady(placeMethods.find(savedPlace.id.get), timeout(Span(5, Seconds))) { foundPlace =>
            foundPlace mustBe Option(place.copy(id = foundPlace.get.id,
              description = Some("<div class='column large-12'>Ancienne usine</div>")))
          }
        } finally {
          whenReady(placeMethods.delete(savedPlace.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    "all be found, sorted by distance" in {
      val here = geographicPointMethods.stringToGeographicPoint("5.4,5.6").get
      val place = Place(None, "test2", Some("123"), None)
      val place2 = Place(None, "test3", Some("1234"), Option(here))
      val place3 = Place(None, "test4", Some("12345"), Option(geographicPointMethods.stringToGeographicPoint("50.4,50.6").get))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        whenReady(placeMethods.save(place2), timeout(Span(2, Seconds))) { savedPlace2 =>
          whenReady(placeMethods.save(place3), timeout(Span(2, Seconds))) { savedPlace3 =>
            try {
              whenReady(placeMethods.findNear(here, 10000, 0), timeout(Span(5, Seconds))) { foundPlaces =>

                foundPlaces should contain inOrder(savedPlace2, savedPlace3, savedPlace) /*place.copy(id = foundPlace.get.id,
                  description = Some("<div class='column large-12'>Ancienne usine</div>")))*/
              }
            } finally {
              whenReady(placeMethods.delete(savedPlace.id.get), timeout(Span(5, Seconds))) {
                _ mustBe 1
              }
              whenReady(placeMethods.delete(savedPlace2.id.get), timeout(Span(5, Seconds))) {
                _ mustBe 1
              }
              whenReady(placeMethods.delete(savedPlace3.id.get), timeout(Span(5, Seconds))) {
                _ mustBe 1
              }
            }
          }
        }
      }
    }

    /*"be saved with its address and deleted in database" in {
      val address = Address(None, None, Some("privas"), Some("07000"), Some("avignas"))
      val place = Place(None, "test", None, None, None, None, Some(9099), None, None, address = Option(address))

      whenReady(placeMethods.save(place), timeout(Span(5, Seconds))) { savedPlace =>
        try {
          whenReady(placeMethods.find(savedPlace.id.get), timeout(Span(5, Seconds))) { foundPlace =>
            foundPlace mustBe
              Option(place.copy(id = savedPlace.id,
                address = Option(address.copy(geographicPoint = Some("(44.7053439,4.596782999999999)")))))
          }
        } finally {
          placeMethods.delete(savedPlace.id.get)
        }
      }
    }*/

    "be followed and unfollowed by a user" in {
      val loginInfo: LoginInfo = LoginInfo("providerId", "providerKey")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("email"),
        avatarURL = Option("avatarUrl"))
      val place = Place(None, "test", Some("123"), None,
        Some("""Ancienne usine"""),
        Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        whenReady(userDAOImpl.save(user), timeout(Span(2, Seconds))) { userSaved =>
          whenReady(placeMethods.followByPlaceId(UserPlaceRelation(uuid, savedPlace.id.get))) { resp =>
            whenReady(placeMethods.isFollowed(UserPlaceRelation(uuid, savedPlace.id.get)), timeout(Span(5, Seconds))) { resp1 =>
             resp1 mustBe true
              whenReady(placeMethods.unfollow(UserPlaceRelation(uuid, savedPlace.id.get)), timeout(Span(5, Seconds))) { resp2 =>
                resp2 mustBe 1
                userDAOImpl.delete(uuid)
                placeMethods.delete(savedPlace.id.get)
              }
            }
          }
        }
      }
    }

    "not be able to be followed twice" in {
      val loginInfo: LoginInfo = LoginInfo("providerId", "providerKey")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("email"),
        avatarURL = Option("avatarUrl"))
      val place = Place(None, "test", Some("123"), None,
        Some("""Ancienne usine"""),
        Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        whenReady(userDAOImpl.save(user), timeout(Span(2, Seconds))) { userSaved =>
          whenReady(placeMethods.followByPlaceId(UserPlaceRelation(uuid, savedPlace.id.get))) { resp =>
            try {
              Await.result(placeMethods.followByPlaceId(UserPlaceRelation(uuid, savedPlace.id.get)), 3 seconds)
            } catch {
              case e: PSQLException =>

                e.getSQLState mustBe utilities.UNIQUE_VIOLATION
            } finally {
              whenReady(placeMethods.unfollow(UserPlaceRelation(uuid, savedPlace.id.get)), timeout(Span(5, Seconds))) { resp2 =>
                resp2 mustBe 1
                userDAOImpl.delete(uuid)
                placeMethods.delete(savedPlace.id.get)
              }
            }
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
          try {
            whenReady(placeMethods.find(placeId), timeout(Span(5, Seconds))) { foundPlace =>
              foundPlace.get.linkedOrganizerId mustBe Some(savedOrganizer.organizer.id.get)
            }
          } finally {
            whenReady(placeMethods.delete(placeId), timeout(Span(5, Seconds))) { resp =>
              resp mustBe 1
              whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
            }
          }
        }
      }
    }

    "save and delete its relation with an event" in {
      val place = Place(None, "test1", Some("1231"), None, Some("""Ancienne usine1"""),
        Some("transbordeur.fr"), Some(90999), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))

      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { tryPlace =>
        val placeId = tryPlace.id.get
        try {
          whenReady(placeMethods.find(placeId), timeout(Span(5, Seconds))) { foundPlace =>
            foundPlace shouldEqual Option(place.copy(id = Option(placeId),
              description = Some("<div class='column large-12'>Ancienne usine1</div>")))
            whenReady(placeMethods.saveEventRelation(EventPlaceRelation(1L, placeId)),
              timeout(Span(5, Seconds))) { resp =>

              resp mustBe 1

              whenReady(placeMethods.findAllByEvent(1L), timeout(Span(5, Seconds))) { places =>

                places should not be empty

                whenReady(eventMethods.findAllByPlace(placeId), timeout(Span(5, Seconds))) { events =>

                  events should not be empty
                }
              }
            }
          }
        } finally {
          whenReady(placeMethods.deleteEventRelation(EventPlaceRelation(eventId = 1L, placeId = placeId)),
            timeout(Span(5, Seconds))) { resp =>

            resp mustBe 1

            whenReady(placeMethods.delete(placeId), timeout(Span(5, Seconds))) { resp1 =>

              resp1 mustBe 1
            }
          }
        }
      }
    }

    "get a new place by facebookId when saving new event by facebookId" in {
      whenReady(eventMethods.saveFacebookEventByFacebookId("933514060052903"), timeout(Span(5, Seconds))) { event =>
        whenReady(placeMethods.getPlaceByFacebookId("836137029786070"), timeout(Span(5, Seconds))) { place =>

          place.name mustBe "Akwaba Coop Culturelle"

          whenReady(placeMethods.delete(place.id.get), timeout(Span(5, Seconds))) { _ mustBe 1}
          whenReady(eventMethods.delete(event.id.get), timeout(Span(5, Seconds))) { _ mustBe 1}
        }
      }
    }

    "find id by facebookId" in {
      whenReady(placeMethods.findIdByFacebookId("776137029786070"), timeout(Span(5, Seconds))) {
        _ mustBe Some(2)
      }
    }

    "find place on facebook" in {
      whenReady(placeMethods.getPlaceOnFacebook("836137029786070"), timeout(Span(5, Seconds))) {
        _.name mustBe "Akwaba Coop Culturelle"
      }
    }

    "be followed by facebookId" in {
      whenReady(placeMethods.followByFacebookId(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), "776137029786070"),
        timeout(Span(5, Seconds))) {
        _ mustBe 1
      }
    }

    "be found near city" in {
      whenReady(placeMethods.findNearCity("lyon", 10, 0),
        timeout(Span(5, Seconds))) {
        _ should contain inOrder (Place(Some(3),"Test1",Some("666137029786070"),
          Some(geographicPointMethods.stringToGeographicPoint("45.783808,4.860598").get),None,None,None,None,None,None,None),
          Place(Some(2),"Test",Some("776137029786070"),Some(geographicPointMethods.stringToGeographicPoint(
              "45.783808,562818797362720700000000000000000000000000000000000000000000000000000000000000").get),
            None,None,None,None,None,None,None))
      }
    }
    /*
    findNearCity
    */
  }
}
