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
        whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { foundEvent =>

          foundEvent.get mustEqual
            EventWithRelations(event.event.copy(
              id = Some(savedEvent.id.get),
              startTime = foundEvent.get.event.startTime,
              endTime =  foundEvent.get.event.endTime))
        }
        whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) {

          _ mustBe 1
        }
      }
    }

    "be saved with relations and deleted in database" in {
      val artists = Vector(ArtistWithWeightedGenres(Artist(None, None, "nameEventRelations", facebookUrl = "saveEventRelations")))
      val organizers = Vector(OrganizerWithAddress(Organizer(None, None, "nameEventRelations")))
      val addresses = Vector(Address(None, None, Option("cityEventRelations")))
      val places = Vector(PlaceWithAddress(Place(name = "nameEventRelations")))
      val genres = Vector(Genre(name = "nameEventRelations"))
      val event = EventWithRelations(
        event = Event(None, None, isPublic = true, isActive = true, "nameEventRelations", None, None,
        new DateTime(), None, 16, None, None, None),
        artists = artists,
        organizers = organizers,
        addresses = addresses,
        places = places,
        genres = genres)

      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { maybeFoundEvent =>
          val foundEvent = maybeFoundEvent.get

          foundEvent.event mustEqual event.event.copy(id = foundEvent.event.id)

          foundEvent.organizers mustBe Vector(OrganizerWithAddress(event.organizers.head.organizer.copy(id = foundEvent.organizers.head.organizer.id)))

          foundEvent.artists mustBe Vector(ArtistWithWeightedGenres(event.artists.head.artist.copy(id = foundEvent.artists.head.artist.id)))

          foundEvent.places mustBe Vector(PlaceWithAddress(event.places.head.place.copy(id = foundEvent.places.head.place.id)))

          foundEvent.addresses mustBe Vector(event.addresses.head.copy(
            id = foundEvent.addresses.head.id,
            city = Option(event.addresses.head.city.get.toLowerCase)))

          foundEvent.genres mustBe Vector(event.genres.head.copy(id = foundEvent.genres.head.id))
        }
        whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) {

          _ mustBe 1
        }
      }
    }

    "be followed and unfollowed by a user" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name1",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description1"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(eventMethods.follow(UserEventRelation(userUUID, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
          whenReady(eventMethods.isFollowed(UserEventRelation(userUUID, savedEvent.id.get)), timeout(Span(5, Seconds))) { response1 =>

            response1 mustBe true
          }
        }
        whenReady(eventMethods.unfollow(UserEventRelation(userUUID, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>

          response mustBe 1
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
          whenReady(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            try {
              Await.result(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), 3 seconds)
            } catch {
              case e: PSQLException =>

                e.getSQLState mustBe utilities.UNIQUE_VIOLATION
            }
          }
        }
      }
    }

    "find all events by genre" in {
      whenReady(eventMethods.findAllByGenre("genreTest0", geographicPointMethods.stringToGeographicPoint("5.4,5.6").get,
        offset = 0, numberToReturn = 100000), timeout(Span(5, Seconds))) { eventsByGenre =>

        eventsByGenre map (_.event.name) must contain("name0")
      }
    }

    "find all events by place" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(), None, 16, None, None, None))
      val place = Place(None, "name", Some("12345"), None, None, None, None, None, None, None)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(placeMethods.save(place), timeout(Span(5, Seconds))) { savedPlace =>
          whenReady(placeMethods.saveEventRelation(EventPlaceRelation(savedEvent.id.get, savedPlace.id.get)),
            timeout(Span(5, Seconds))) { placeEventRelation =>

            placeEventRelation mustBe 1

            whenReady(eventMethods.findAllByPlace(savedPlace.id.get), timeout(Span(5, Seconds))) { eventsByPlace =>

              eventsByPlace must contain(EventWithRelations(
                event = savedEvent,
                places = Vector(PlaceWithAddress(savedPlace))))
            }
          }
        }
      }
    }

    "find all events by place sorted by date" in {
      whenReady(eventMethods.findAllByPlace(2), timeout(Span(5, Seconds))) { eventsByPlace =>

        assert(eventsByPlace.size > 2)

        val startTimes = eventsByPlace map (_.event.startTime.getMillis.toDouble)
        val reverseStartTimes = startTimes.toList.reverse

        assert(isOrdered(reverseStartTimes))
      }
    }

    "return passed events for a place" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        None, None, new DateTime(0), None, 16, None, None, None))
      val place = Place(None, "name", Some("12345"), None, None, None, None, None, None, None)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(placeMethods.saveWithAddress(PlaceWithAddress(place, None)), timeout(Span(5, Seconds))) { savedPlace =>
          whenReady(placeMethods.saveEventRelation(EventPlaceRelation(savedEvent.id.get, savedPlace.place.id.get)),
            timeout(Span(5, Seconds))) { placeEventRelation =>

            placeEventRelation mustBe 1

            whenReady(eventMethods.findAllByPlace(savedPlace.place.id.get), timeout(Span(5, Seconds))) { eventsByPlace =>

              eventsByPlace must not contain savedEvent

              whenReady(eventMethods.findAllPassedByPlace(savedPlace.place.id.get), timeout(Span(5, Seconds))) { passedEventsByPlace =>

                passedEventsByPlace must contain(EventWithRelations(savedEvent, places = Vector(savedPlace)))
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
          whenReady(artistMethods.saveEventRelation(EventArtistRelation(savedEvent.id.get, savedArtist.id.get)),
            timeout(Span(5, Seconds))) { artistEventRelation =>

            artistEventRelation mustBe 1

            whenReady(eventMethods.findAllByArtist(savedArtist.facebookUrl), timeout(Span(5, Seconds))) { eventsByArtist =>

              eventsByArtist must
                contain(EventWithRelations(event = savedEvent, artists = Vector(ArtistWithWeightedGenres(savedArtist))))
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
          whenReady(artistMethods.saveEventRelation(EventArtistRelation(savedEvent.id.get, savedArtist.id.get)),
            timeout(Span(5, Seconds))) { artistEventRelation =>

            artistEventRelation mustBe 1

            whenReady(eventMethods.findAllByArtist(savedArtist.facebookUrl), timeout(Span(5, Seconds))) { eventsByArtist =>

              eventsByArtist must not contain savedEvent

              whenReady(eventMethods.findAllPassedByArtist(savedArtist.id.get), timeout(Span(5, Seconds))) { passedEventsByArtist =>

                passedEventsByArtist must
                  contain(EventWithRelations(event = savedEvent, artists = Vector(ArtistWithWeightedGenres(savedArtist))))
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
          whenReady(organizerMethods.saveEventRelation(EventOrganizerRelation(savedEvent.id.get, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { organizerEventRelation =>

            organizerEventRelation mustBe 1

            whenReady(eventMethods.findAllByOrganizer(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { eventsByOrganizer =>

              eventsByOrganizer must
                contain(EventWithRelations(event = savedEvent, organizers = Vector(OrganizerWithAddress(savedOrganizer.organizer))))
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
          whenReady(organizerMethods.saveEventRelation(EventOrganizerRelation(savedEvent.id.get, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { organizerEventRelation =>

            organizerEventRelation mustBe 1

            whenReady(eventMethods.findAllByOrganizer(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { eventsByOrganizer =>

              eventsByOrganizer must not contain savedEvent

              whenReady(eventMethods.findAllPassedByOrganizer(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {
                passedEventsByOrganizer =>

                passedEventsByOrganizer must contain(
                  EventWithRelations(event = savedEvent, organizers = Vector(OrganizerWithAddress(savedOrganizer.organizer))))
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
      whenReady(eventMethods.findEventOnFacebookByFacebookId("809097205831013"), timeout(Span(10, Seconds))) { event =>
        event.get.event.name mustBe "ANNULÉ /// Mad Professor vs Prince Fatty - Dub Attack Tour"
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
            }
          }
        }
      }
    }

    "have the genre of its artists" in {
      whenReady(eventMethods.findEventOnFacebookByFacebookId("758796230916379"), timeout(Span(10, Seconds))) { eventWithRelations =>
        eventWithRelations.get.genres should contain allOf(Genre(None, "hip", 'a'), Genre(None, "hop", 'a'))
      }
    }
    
    "find events in period near" in {
      whenReady(eventMethods.findInPeriodNear(
        hourInterval = 100000,
        geographicPointMethods.stringToGeographicPoint("45, 4").get,
        numberToReturn = 1,
        offset = 0), timeout(Span(5, Seconds))) { events =>

        events should not be empty

        assert(DateTime.now.minusHours(12).compareTo(events.head.event.startTime) < 0)
      }
    }

    "find passed events in period near" in {
      whenReady(eventMethods.findPassedInHourIntervalNear(
        hourInterval = 100000,
        geographicPointMethods.stringToGeographicPoint("45, 4").get,
        numberToReturn = 1,
        offset = 0), timeout(Span(5, Seconds))) { events =>

        events should not be empty

        assert(DateTime.now.compareTo(events.head.event.startTime) > 0)
      }
    }

    "find all containing pattern near a geoPoint" in {
      whenReady(eventMethods.findAllContaining(pattern = "name", geographicPointMethods.stringToGeographicPoint("45, 4").get),
        timeout(Span(5, Seconds))) { events =>

        events should not be empty
      }
    }

     "find all by city pattern" in {
      whenReady(eventMethods.findAllByCityPattern(cityPattern = "lyon"), timeout(Span(5, Seconds))) { events =>

        events should not be empty
      }
    }

    "be found near city" in {
      whenReady(eventMethods.findNearCity("lyon", 10, 0), timeout(Span(5, Seconds))) { events =>
        events.head.event.name mustBe "notPassedEvent"
      }
    }

    //readEventsIdsFromWSResponse
  }
}
