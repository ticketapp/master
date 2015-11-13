import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.joda.time.DateTime
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestOrganizerModel extends GlobalApplicationForModels {

  "An Organizer" must {

    "be saved and deleted in database" in {
      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
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
      val organizer = Organizer(None, Option("facebookId22"), "organizerTest22", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        savedOrganizer mustEqual OrganizerWithAddress(
          organizer = organizer.copy(
            id = Some(savedOrganizer.organizer.id.get),
            description = Some("<div class='column large-12'>description</div>")),
          address = None)
        whenReady(organizerMethods.findAll, timeout(Span(5, Seconds))) { foundOrganizers =>
          foundOrganizers should contain(savedOrganizer)
        }
      }
    }

    "find organizer by facebookId" in {
      val organizer = Organizer(None, Option("facebookId222"), "organizerTest222", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        savedOrganizer mustEqual OrganizerWithAddress(
          organizer = organizer.copy(
            id = Some(savedOrganizer.organizer.id.get),
            description = Some("<div class='column large-12'>description</div>")),
          address = None)
        whenReady(organizerMethods.findIdByFacebookId(savedOrganizer.organizer.facebookId), timeout(Span(5, Seconds))) { foundOrganizer =>
          foundOrganizer mustBe savedOrganizer.organizer.id
        }
      }
    }

    "find organizers near geoPoint" in {
      val organizer = Organizer(None, Option("facebookId2222"), "organizerTest2222", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      val organizer1 = Organizer(None, Option("facebookId22222"), "organizerTest22222", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("6.4,6.6").get))
      val organizer2 = Organizer(None, Option("facebookId222222"), "organizerTest222222", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("7.4,7.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer1, None)), timeout(Span(5, Seconds))) { savedOrganizer1 =>
          whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer2, None)), timeout(Span(5, Seconds))) { savedOrganizer2 =>
            whenReady(organizerMethods.findNear(geographicPointMethods.stringToGeographicPoint("7.4,7.6").get, 10, 0),
              timeout(Span(5, Seconds))) { foundOrganizers =>
              foundOrganizers should contain inOrder(savedOrganizer2, savedOrganizer1, savedOrganizer)
            }
          }
        }
      }
    }

    "be found near city" in {
      whenReady (organizerMethods.findNearCity("lyon", 10, 0),  timeout(Span(5,  Seconds))) { response =>
        val organizer = OrganizerWithAddress(
          organizer = Organizer(Some(3), Some("facebookId1"), "name2", None, None, None, None, None, verified = false,
            None, Some(geographicPointMethods.stringToGeographicPoint("45.783808, 4.860598").get), None),
          address = None)
        val organizer2 = OrganizerWithAddress(
          organizer = Organizer(Some(2), Some("facebookId"), "name1", None, None, None, None, None, verified = false,
            None, Some(geographicPointMethods.stringToGeographicPoint(
              "45.783808,562818797362720700000000000000000000000000000000000000000000000000000000000000").get), None),
          address = None)

        response must contain inOrder(organizer, organizer2)
      }
    }

    "not be saved twice and return the organizerId" in {
      val organizer = Organizer(None, Option("facebookId3"), "organizerTest3", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) {

          _ mustBe savedOrganizer
        }
        whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {

          _ mustBe 1
        }
      }
    }

    "be followed and unfollowed by a user" in {
      val organizer = Organizer(None, Option("facebookId4"), "organizerTest4", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      val loginInfo: LoginInfo = LoginInfo("providerId", "providerKey")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("emaill"),
        avatarURL = Option("avatarUrl"))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
          timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe true
            }
          }
          whenReady(organizerMethods.unfollow(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe 1
            }
          }
        }
      }
    }

    "be followed by facebookId" in {
      val organizer = Organizer(None, Option("facebookId44"), "organizerTest44", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      val loginInfo: LoginInfo = LoginInfo("providerId44", "providerKey44")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName44"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("email44"),
        avatarURL = Option("avatarUrl"))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          whenReady(organizerMethods.followByFacebookId(uuid, savedOrganizer.organizer.facebookId.get),
          timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe true
            }
          }
        }
      }
    }

    "not be followed twice" in {
      val organizer = Organizer(None, Option("facebookId14"), "organizerTest14", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      val loginInfo: LoginInfo = LoginInfo("providerId1", "providerKey1")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName1"),
        lastName = Option("lastName1"),
        fullName = Option("fullName1"),
        email = Option("email1"),
        avatarURL = Option("avatarUrl"))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe true
              try {
                Await.result(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get))
                  , 3 seconds)
              } catch {
                case e: PSQLException =>

                  e.getSQLState mustBe utilities.UNIQUE_VIOLATION
              }
            }
          }
        }
      }
    }

    "be linked to a place if one with the same facebookId already exists" in {
      whenReady (placeMethods.save(Place(None, "Name1", Some("1234567891"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds)))  { tryPlace =>
        val placeId = tryPlace.id.get
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(Organizer(None, Some("1234567891"), "organizerTest2"), None)),
          timeout(Span(5, Seconds))) { savedOrganizer =>
          whenReady(organizerMethods.findById(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {
            case Some(organizerWithAddress: OrganizerWithAddress) =>
              organizerWithAddress.organizer.linkedPlaceId mustBe Some(placeId)
            case _ =>
              throw new Exception("TestOrganizerModel.musBeLinkedToAPlace: error on save or find")
          }
        }
      }
    }

    "save organizer with event relation" in {
      val geoPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get)
      val organizer = OrganizerWithAddress(Organizer(None, None, "organizerTest2", geographicPoint = geoPoint))

      val event = Event(None, None, isPublic = true, isActive = true, "name", geoPoint, None, new DateTime(), None, 16, None, None, None)

      whenReady(eventMethods.save(EventWithRelations(event)), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(organizerMethods.saveWithEventRelation(organizer, savedEvent.id.get), timeout(Span(5, Seconds))) { savedOrganizer =>
          whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { foundEvent =>
            whenReady(organizerMethods.findAllByEventId(foundEvent.get.event.id.get), timeout(Span(5, Seconds))) { foundOrganizers =>

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

        organizerInfos.get.organizer.name mustBe "Le Transbordeur"

        whenReady(organizerMethods.saveWithAddress(organizerInfos.get), timeout(Span(5, Seconds))) { savedOrganizer =>
          savedOrganizer.organizer.name mustBe "Le Transbordeur"
          savedOrganizer.address.get mustBe Address(savedOrganizer.address.get.id,
            Option(geographicPointMethods.stringToGeographicPoint("45.7839103,4.860398399999999").get),
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
      val eventOrganizerRelations = Seq(EventOrganizerRelation(2, 3), EventOrganizerRelation(3, 3))
      whenReady (organizerMethods.saveEventRelations(eventOrganizerRelations), timeout(Span(5, Seconds))) { response =>
        response mustBe true
      }
    }
  }
}

