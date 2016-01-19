import addresses.Address
import artistsDomain.{Artist, ArtistWithWeightedGenres}
import database.{EventArtistRelation, EventOrganizerRelation, EventPlaceRelation}
import eventsDomain.{Event, EventWithRelations}
import genresDomain.Genre
import org.joda.time.DateTime
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.{Place, PlaceWithAddress}
import testsHelper.GlobalApplicationForModels

import scala.language.postfixOps


class TestEventModel extends GlobalApplicationForModels {

  "An event" must {

    "be saved and deleted in database" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name",
        geographicPointMethods.stringToTryPoint("5.4,5.6").get,
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
      val artists = Vector(ArtistWithWeightedGenres(
        artist = Artist(name = "nameEventRelations", facebookUrl = "saveEventRelations")))
      val organizers = Vector(OrganizerWithAddress(Organizer(None, None, "nameEventRelations")))
      val addresses = Vector(Address(id = None, city = Option("cityEventRelations")))
      val places = Vector(PlaceWithAddress(Place(name = "nameEventRelations")))
      val genres = Vector(Genre(name = "nameeventrelations"))
      val event = EventWithRelations(
        event = Event(
          isPublic = true,
          isActive = true,
          name = "nameEventRelations",
          startTime = new DateTime(),
          endTime = None,
          ageRestriction = 16,
          tariffRange = None,
          ticketSellers = None,
          imagePath = None),
        artists = artists,
        organizers = organizers,
        addresses = addresses,
        places = places,
        genres = genres)

      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { maybeFoundEvent =>
          val foundEvent = maybeFoundEvent.get

          foundEvent.event mustEqual event.event.copy(id = foundEvent.event.id)

          foundEvent.organizers mustBe
            Vector(OrganizerWithAddress(event.organizers.head.organizer.copy(id = foundEvent.organizers.head.organizer.id)))

          foundEvent.artists mustBe
            Vector(ArtistWithWeightedGenres(event.artists.head.artist.copy(id = foundEvent.artists.head.artist.id)))

          foundEvent.places mustBe Vector(PlaceWithAddress(event.places.head.place.copy(id = foundEvent.places.head.place.id)))

          foundEvent.addresses mustBe Vector(event.addresses.head.copy(
            id = foundEvent.addresses.head.id,
            city = Option(event.addresses.head.city.get.toLowerCase)))

          foundEvent.genres mustBe Vector(event.genres.head.copy(
            id = foundEvent.genres.head.id,
            name = foundEvent.genres.head.name.toLowerCase))
        }
        whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) {
          _ mustBe 1
        }
      }
    }

    "find all events by genre" in {
      whenReady(eventMethods.findAllByGenre("genreTest0", geographicPointMethods.stringToTryPoint("5.4,5.6").get,
        offset = 0, numberToReturn = 100000), timeout(Span(5, Seconds))) { eventsByGenre =>

        eventsByGenre map (_.event.name) must contain("name0")
      }
    }

    "find all events by place" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        geographicPointMethods.stringToTryPoint("5.4,5.6").get,
        Option("description3"), new DateTime(), None, 16, None, None, None))
      val place = Place(
        name = "name",
        facebookId = Some("12345"))
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
      whenReady(eventMethods.findAllByPlace(100), timeout(Span(5, Seconds))) { eventsByPlace =>

        assert(eventsByPlace.size > 2)

        val startTimes = eventsByPlace map (_.event.startTime.getMillis.toDouble)
        val reverseStartTimes = startTimes.toList.reverse

        assert(isOrdered(reverseStartTimes))
      }
    }

    "return passed events for a place" in {
      val event = EventWithRelations(
        event = Event(
          isPublic = true,
          isActive = true,
          name = "name3",
          startTime = new DateTime(0),
          endTime = None,
          ageRestriction = 16,
          tariffRange = None,
          ticketSellers = None,
          imagePath = None))
      val place = Place(name = "name", facebookId = Some("12345"))
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
        geographicPointMethods.stringToTryPoint("5.4,5.6").get,
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
        geographicPointMethods.stringToTryPoint("5.4,5.6").get,
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
        geographicPointMethods.stringToTryPoint("5.4,5.6").get,
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
        geographicPointMethods.stringToTryPoint("5.4,5.6").get,
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
      val expectedGeoPoint = geographicPointMethods.optionStringToPoint(Option("45.7408394,4.8499501"))
      whenReady(eventMethods.getEventOnFacebookByFacebookId("985240908201444"), timeout(Span(10, Seconds))) { event =>
        event.get.event.name mustBe "ENCORE w/ OCTAVE ONE live — MOJO — MOONRISE HILL CREW"
        event.get.geographicPoint mustBe expectedGeoPoint
        event.get.addresses mustBe Vector(Address(
          id = Some(3),
          geographicPoint = expectedGeoPoint,
          city = Some("lyon"),
          zip = Some("69007"),
          street = Some("rue paul vivier / rue de cronstadt")))
      }
    }

    "find nearest events" in {
      val here = geographicPointMethods.stringToTryPoint("45, 4").get
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name3",
        geographicPointMethods.stringToTryPoint("46, 4").get,
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val event1 = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name4", here,
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val event2 = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "name5",
        geographicPointMethods.stringToTryPoint("66, 4").get,
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None))
      val event3 = EventWithRelations(
        event = Event(
          isPublic = true,
          isActive = true,
          name = "name5",
          startTime = new DateTime(),
          endTime = None,
          ageRestriction = 16))
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(eventMethods.save(event1), timeout(Span(5, Seconds))) { savedEvent1 =>
          whenReady(eventMethods.save(event2), timeout(Span(5, Seconds))) { savedEvent2 =>
            whenReady(eventMethods.save(event3), timeout(Span(5, Seconds))) { savedEvent3 =>
              whenReady(eventMethods.findNear(here, numberToReturn = 10000, offset = 0),
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
      whenReady(eventMethods.getEventOnFacebookByFacebookId("758796230916379"), timeout(Span(10, Seconds))) { eventWithRelations =>
        eventWithRelations.get.genres should contain allOf(Genre(None, "hip", 'a'), Genre(None, "hop", 'a'))
      }
    }

    "find events in period near" in {
      whenReady(eventMethods.findInPeriodNear(
        hourInterval = 4380000,
        geographicPointMethods.stringToTryPoint("45.7579555,4.8351209").get,
        numberToReturn = 10,
        offset = 0), timeout(Span(5, Seconds))) { events =>

        events map { _.event.name } should contain inOrder("notPassedEvent2", "name0", "notPassedEvent", "inProgressEvent")
        events map { _.event.name } should not contain allOf("eventPassed", "eventPassedWithoutEndTime")

        assert(DateTime.now.minusHours(12).compareTo(events.head.event.startTime) < 0)
      }
    }

    "find passed events in period near" in {
      whenReady(eventMethods.findPassedInHourIntervalNear(
        hourInterval = 100000,
        geographicPointMethods.stringToTryPoint("45, 4").get,
        numberToReturn = 1,
        offset = 0), timeout(Span(5, Seconds))) { events =>

        events should not be empty

        assert(DateTime.now.compareTo(events.head.event.startTime) > 0)
      }
    }

    "find all containing pattern near a geoPoint" in {
      whenReady(eventMethods.findAllContaining(pattern = "name", geographicPointMethods.stringToTryPoint("45, 4").get),
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
        events.head.event.name mustBe "notPassedEvent2"
      }
    }

    "get users by event's facebook id" in {
      whenReady(eventMethods.getUsersByEventFacebookId("1740865666143460"), timeout(Span(5, Seconds))) { users =>
        users should not be empty
      }
    }

    //readEventsIdsFromWSResponse
  }
}
