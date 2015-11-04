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

class TestEventModel extends GlobalApplicationForModels {

  "An event" must {

    "be saved and deleted in database" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        try {
          whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { foundEvent =>
            foundEvent.get mustEqual
              EventWithRelations(event.event.copy(
                id = Some(savedEvent.id.get),
                startTime = foundEvent.get.event.startTime,
                endTime =  foundEvent.get.event.endTime))
          }
        } finally {
          whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) {
            _ mustBe 1
          }
        }
      }
    }

    "be followed and unfollowed by a user" in {
      val loginInfo: LoginInfo = LoginInfo("providerId", "providerKey")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("emailFollowEvent"),
        avatarURL = Option("avatarUrl"))
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name1",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description1"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
              whenReady(eventMethods.isFollowed(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe true
              }
            }
          } finally {
            whenReady(eventMethods.unfollow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
              response mustBe 1
              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) {
                  _ mustBe 1
                }
              }
            }
          }
        }
      }
    }

    "not be followed twice" in {
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
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name2",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description2"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              try {
                Await.result(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), 3 seconds)
              } catch {
                case e: PSQLException =>

                  e.getSQLState mustBe utilities.UNIQUE_VIOLATION
              }
            }
          } finally {
            whenReady(eventMethods.unfollow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
              response mustBe 1
              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>
                response1 mustBe 1
                whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { response2 =>
                  response2 mustBe 1
                }
              }
            }
          }
        }
      }
    }

    "find all events by genre" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val genre = Genre(None, "rockiedockie", 'r')
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { savedGenre =>
          try {
            whenReady(genreMethods.saveEventRelation(EventGenreRelation(savedEvent.id.get, savedGenre.id.get)),
              timeout(Span(5, Seconds))) { genreEventRelation =>

              genreEventRelation mustBe 1

              whenReady(
                eventMethods.findAllByGenre("rockiedockie",
                  geographicPointMethods.stringToGeographicPoint("5.4,5.6").get, offset = 0, numberToReturn = 100000),
                timeout(Span(5, Seconds))) { eventsByGenre =>

                eventsByGenre must contain(EventWithRelations(event = savedEvent, genres = Vector(savedGenre)))
              }
            }
          } finally {
            whenReady(genreMethods.deleteEventRelation(EventGenreRelation(savedEvent.id.get, savedGenre.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(genreMethods.delete(savedGenre.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "find all events by place" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(), None, 16, None, None, None))
      val place = Place(None, "name", Some("12345"), None, None, None, None, None, None, None)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(placeMethods.save(place), timeout(Span(5, Seconds))) { savedPlace =>
          try {
            whenReady(placeMethods.saveEventRelation(EventPlaceRelation(savedEvent.id.get, savedPlace.id.get)),
              timeout(Span(5, Seconds))) { placeEventRelation =>

              placeEventRelation mustBe 1

              whenReady(eventMethods.findAllByPlace(savedPlace.id.get), timeout(Span(5, Seconds))) { eventsByPlace =>

                eventsByPlace must contain(EventWithRelations(
                  event = savedEvent,
                  places = Vector(savedPlace)))
              }
            }
          } finally {
            whenReady(placeMethods.deleteEventRelation(EventPlaceRelation(savedEvent.id.get, savedPlace.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(placeMethods.delete(savedPlace.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "return passed events for a place" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        None, None, new DateTime(0), None, 16, None, None, None))
      val place = Place(None, "name", Some("12345"), None, None, None, None, None, None, None)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(placeMethods.save(place), timeout(Span(5, Seconds))) { savedPlace =>
          try {
            whenReady(placeMethods.saveEventRelation(EventPlaceRelation(savedEvent.id.get, savedPlace.id.get)),
              timeout(Span(5, Seconds))) { placeEventRelation =>

              placeEventRelation mustBe 1

              whenReady(eventMethods.findAllByPlace(savedPlace.id.get), timeout(Span(5, Seconds))) { eventsByPlace =>

                eventsByPlace must not contain savedEvent

                whenReady(eventMethods.findAllPassedByPlace(savedPlace.id.get), timeout(Span(5, Seconds))) { passedEventsByPlace =>

                  passedEventsByPlace must contain(EventWithRelations(savedEvent, places = Vector(savedPlace)))
                }
              }
            }
          } finally {
            whenReady(placeMethods.deleteEventRelation(EventPlaceRelation(savedEvent.id.get, savedPlace.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(placeMethods.delete(savedPlace.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "return events linked to an artist" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookId123"), "artistTest123", Option("imagePath"), Option("description"),
        "facebookUrl123"), Vector.empty)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
          try {
            whenReady(artistMethods.saveEventRelation(EventArtistRelation(savedEvent.id.get, savedArtist.id.get)),
              timeout(Span(5, Seconds))) { artistEventRelation =>

              artistEventRelation mustBe 1

              whenReady(eventMethods.findAllByArtist(savedArtist.facebookUrl), timeout(Span(5, Seconds))) { eventsByArtist =>

                eventsByArtist must contain(EventWithRelations(event = savedEvent, artists = Vector(savedArtist)))
              }
            }
          } finally {
            whenReady(artistMethods.deleteEventRelation(EventArtistRelation(savedEvent.id.get, savedArtist.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(artistMethods.delete(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "return passed events for an artist" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(0), Option(new DateTime(0)), 16, None, None, None))
      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookId1234"), "artistTest1234", Option("imagePath"),
        Option("description"), "facebookUrl1234"), Vector.empty)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
          try {
            whenReady(artistMethods.saveEventRelation(EventArtistRelation(savedEvent.id.get, savedArtist.id.get)),
              timeout(Span(5, Seconds))) { artistEventRelation =>

              artistEventRelation mustBe 1

              whenReady(eventMethods.findAllByArtist(savedArtist.facebookUrl), timeout(Span(5, Seconds))) { eventsByArtist =>

                eventsByArtist must not contain savedEvent

                whenReady(eventMethods.findAllPassedByArtist(savedArtist.id.get), timeout(Span(5, Seconds))) { passedEventsByArtist =>

                  passedEventsByArtist must contain(EventWithRelations(event = savedEvent, artists = Vector(savedArtist)))
                }
              }
            }
          } finally {
            whenReady(artistMethods.deleteEventRelation(EventArtistRelation(savedEvent.id.get, savedArtist.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(artistMethods.delete(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "return events linked to an organizer" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val organizer = Organizer(None, Option("facebookId10"), "organizerTest2")
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
          try {
            whenReady(organizerMethods.saveEventRelation(EventOrganizerRelation(savedEvent.id.get, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { organizerEventRelation =>

              organizerEventRelation mustBe 1

              whenReady(eventMethods.findAllByOrganizer(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { eventsByOrganizer =>

                eventsByOrganizer must contain(EventWithRelations(event = savedEvent, organizers = Vector(savedOrganizer.organizer)))
              }
            }
          } finally {
            whenReady(organizerMethods.deleteEventRelation(EventOrganizerRelation(savedEvent.id.get, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1
              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "return passed events for an organizer" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(0), Option(new DateTime(0)), 16, None, None, None))
      val organizer = Organizer(None, Option("facebookId101"), "organizerTest21")
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
          try {
            whenReady(organizerMethods.saveEventRelation(EventOrganizerRelation(savedEvent.id.get, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { organizerEventRelation =>

              organizerEventRelation mustBe 1

              whenReady(eventMethods.findAllByOrganizer(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { eventsByOrganizer =>

                eventsByOrganizer must not contain savedEvent

                whenReady(eventMethods.findAllPassedByOrganizer(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {
                  passedEventsByOrganizer =>

                  passedEventsByOrganizer must contain(
                    EventWithRelations(event = savedEvent, organizers = Vector(savedOrganizer.organizer)))
                }
              }
            }
          } finally {
            whenReady(organizerMethods.deleteEventRelation(EventOrganizerRelation(savedEvent.id.get, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "return events facebook id for a place facebook id" in {
      whenReady(eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId("117030545096697"), timeout(Span(2, Seconds))) {
        _ should not be empty
      }
    }

    "find a complete event by facebookId" in {
      whenReady(eventMethods.findEventOnFacebookByFacebookId("809097205831013"), timeout(Span(5, Seconds))) { event =>
        event.event.name mustBe "ANNULÉ /// Mad Professor vs Prince Fatty - Dub Attack Tour"
      }
    }

    "find nearest events" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("46, 4").get),
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val event1 = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name4",
        Option(geographicPointMethods.stringToGeographicPoint("45, 4").get),
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val event2 = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name5",
        Option(geographicPointMethods.stringToGeographicPoint("120, 120").get),
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val event3 = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name5", None, None,
        new DateTime(), None, 16, None, None, None))
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(eventMethods.save(event1), timeout(Span(5, Seconds))) { savedEvent1 =>
          whenReady(eventMethods.save(event2), timeout(Span(5, Seconds))) { savedEvent2 =>
            whenReady(eventMethods.save(event3), timeout(Span(5, Seconds))) { savedEvent3 =>
              try {
                whenReady(
                  eventMethods.findNear(
                    geographicPointMethods.stringToGeographicPoint("45, 4").get, numberToReturn = 10000, offset = 0),
                  timeout(Span(5, Seconds))) { eventsSeq =>

                  eventsSeq should contain inOrder(
                    EventWithRelations(savedEvent1),
                    EventWithRelations(savedEvent),
                    EventWithRelations(savedEvent2),
                    EventWithRelations(savedEvent3))
                }
              } finally {
                eventMethods.delete(savedEvent.id.get)
                eventMethods.delete(savedEvent1.id.get)
                eventMethods.delete(savedEvent2.id.get)
                eventMethods.delete(savedEvent3.id.get)
              }
            }
          }
        }
      }
    }

    "have the genre of its artists" in {
      whenReady(eventMethods.findEventOnFacebookByFacebookId("758796230916379"), timeout(Span(5, Seconds))) { event =>
        event.genres should contain allOf (Genre(None, "hip", 'a'), Genre(None, "hop", 'a'))
      }
    }

    "be found near city" in {
      whenReady(eventMethods.findNearCity("lyon", 10, 0), timeout(Span(5, Seconds))) { events =>
        events.head.event.name mustBe "notPassedEvent"
      }
    }

    //save with an address
    //get with an address
    //get with all relations see docker
  }
}
