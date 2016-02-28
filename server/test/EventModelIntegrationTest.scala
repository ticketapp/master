import addresses.Address
import artistsDomain.{Artist, ArtistWithWeightedGenres}
import database.MyPostgresDriver.api._
import database.{EventArtistRelation, EventOrganizerRelation, EventPlaceRelation}
import eventsDomain.{Event, EventWithRelations}
import genresDomain.Genre
import org.joda.time.DateTime
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.{Place, PlaceWithAddress}
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class EventModelIntegrationTest extends GlobalApplicationForModelsIntegration {

  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
           VALUES(true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
           VALUES(true, true, 'nearestEvent', current_timestamp, '010100000000000000000055C00000000000006E40');
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
           VALUES(true, true, 'later', timestamp WITH TIME ZONE '2040-08-24 14:00:00', '01010000000927F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'laterThanLater', timestamp WITH TIME ZONE '2042-08-24 14:00:00', '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name66', timestamp WITH TIME ZONE '2042-08-24 14:00:00',
          ST_GeomFromText('POINT(46 4)', 4326));
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name4', timestamp WITH TIME ZONE '2042-08-24 14:00:00',
          ST_GeomFromText('POINT(45 4)', 4326));
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name5', timestamp WITH TIME ZONE '2042-08-24 14:00:00',
          ST_GeomFromText('POINT(66 4)', 4326));
        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name6', timestamp WITH TIME ZONE '2042-08-24 14:00:00',
          ST_GeomFromText('POINT(-84 30)', 4326));

        INSERT INTO genres(name, icon) VALUES('genretest0', 'a');
        INSERT INTO eventsgenres(eventid, genreid) VALUES((SELECT eventId FROM events WHERE name = 'name0'), 1);

        INSERT INTO places(placeid, name) VALUES(100, 'name0');
        INSERT INTO places(placeid, name) VALUES(200, 'name1');
        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'name0'), (SELECT placeid FROM places WHERE name = 'name0'));
        INSERT INTO eventsplaces(eventid, placeid)
         VALUES((SELECT eventId FROM events WHERE name = 'name0'), (SELECT placeid FROM places WHERE name = 'name1'));
        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'later'), (SELECT placeid FROM places WHERE name = 'name1'));
        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'laterThanLater'), (SELECT placeid FROM places WHERE name = 'name1'));

        INSERT INTO addresses(city) VALUES('lyon');
        INSERT INTO eventsaddresses(eventid, addressid) VALUES((SELECT eventId FROM events WHERE name = 'name0'),
          (SELECT addressid FROM addresses WHERE city = 'lyon'));
        INSERT INTO frenchcities(city, geographicpoint) VALUES('lyon', '0101000020E6100000ED2B0FD253E446401503249A40711340');"""),
      2.seconds)
  }

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
      val expectedPlace = PlaceWithAddress(Place(
        id = Some(100),
        name = "name0", geographicPoint = geographicPointMethods.stringToTryPoint("-84,30").get))

      whenReady(eventMethods.findAllNotFinishedByPlace(placeId = 100), timeout(Span(5, Seconds))) { eventsByPlace =>

        eventsByPlace.head.places.head mustBe expectedPlace
        eventsByPlace.head.event.name mustBe "name0"
      }
    }

    "find all events by place sorted by date" in {
      whenReady(eventMethods.findAllNotFinishedByPlace(placeId = 200), timeout(Span(5, Seconds))) { eventsByPlace =>

        assert(eventsByPlace.size > 2)

        val startTimes = eventsByPlace map (_.event.startTime.getMillis.toDouble)
        val reverseStartTimes = startTimes.toList.reverse

        assert(isOrdered(reverseStartTimes))
      }
    }

    "find passed events for a place" in {
      val event = EventWithRelations(event = Event(name = "name3", startTime = new DateTime(0)))
      val place = Place(name = "name", facebookId = Some("12345"))

      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(placeMethods.saveWithAddress(PlaceWithAddress(place, None)), timeout(Span(5, Seconds))) { savedPlace =>
          whenReady(placeMethods.saveEventRelation(EventPlaceRelation(savedEvent.id.get, savedPlace.place.id.get)),
            timeout(Span(5, Seconds))) { placeEventRelation =>

            placeEventRelation mustBe 1

            whenReady(eventMethods.findAllNotFinishedByPlace(savedPlace.place.id.get), timeout(Span(5, Seconds))) { eventsByPlace =>

              eventsByPlace must not contain savedEvent

              whenReady(eventMethods.findAllPassedByPlace(savedPlace.place.id.get), timeout(Span(5, Seconds))) { passedEventsByPlace =>

                passedEventsByPlace must contain(EventWithRelations(savedEvent, places = Vector(savedPlace)))
              }
            }
          }
        }
      }
    }

    "find events linked to an artist" in {
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

    "find passed events for an artist" in {
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

    "find events linked to an organizer" in {
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

    "find passed events for an organizer" in {
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

    "find the event facebook id for a place facebook id" in {
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

      whenReady(eventMethods.findNear(here, numberToReturn = 10000, offset = 0),
        timeout(Span(5, Seconds))) { eventsSeq =>
        eventsSeq.map(_.event.name) should contain inOrder("name4", "name66", "name5", "name6")
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
        geographicPointMethods.stringToTryPoint("-84, 240").get,
        numberToReturn = 10,
        offset = 0), timeout(Span(5, Seconds))) { events =>
        events map (_.event.name) should contain inOrder("nearestEvent" ,"name0", "later")
        events map (_.event.name) should not contain allOf("eventPassed", "eventPassedWithoutEndTime")

        assert(DateTime.now.minusHours(12).compareTo(events.head.event.startTime) < 0)
      }
    }

    "find passed events in period near" in {
      whenReady(eventMethods.findPassedInHourIntervalNear(
        hourInterval = 100000,
        geographicPoint = geographicPointMethods.stringToTryPoint("45, 4").get,
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

        events map(_.event.name) should contain("later")
      }
    }

    //readEventsIdsFromWSResponse
  }
}
