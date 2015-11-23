package jobs

import javax.inject.Inject

import models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.util.control.NonFatal


class Scheduler @Inject()(val eventMethods: EventMethods,
                          val organizerMethods: OrganizerMethods,
                          val artistMethods: ArtistMethods,
                          val trackMethods: TrackMethods,
                          val placeMethods: PlaceMethods,
                          val addressMethods: AddressMethods,
                          val searchGeographicPoint: SearchGeographicPoint) {


  def findEventsForPlacesOneByOne(offset: Long = 0): Unit = placeMethods.findSinceOffset(offset = 0, numberToReturn = 1) map {
    places =>
    places.headOption match {
      case Some(place) =>
        place.place.facebookId match {
          case Some(facebookId) =>
            eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map {
              _.map { eventId =>
                eventMethods.saveFacebookEventByFacebookId(eventId) recover {
                  case NonFatal(e) => Logger.error("Scheduler.findEventsForPlaces: ", e)
                }
              }
            } recover { case NonFatal(e) => Logger.error("Scheduler.findEventsForPlaces: ", e) }
          case _ =>
        }

        Thread.sleep(6000)
        findEventsForPlacesOneByOne(offset + 1)

      case None =>
        Logger.info("Scheduler.findEventsForPlacesOneByOne: DONE :)")
    }
  }

  def findEventsForOrganizersOneByOne(offset: Long = 0): Unit = organizerMethods.findSinceOffset(offset, numberToReturn = 1) map {
    organizers =>
    organizers.headOption match {
      case Some(organizer) =>
        organizer.organizer.facebookId match {
          case Some(facebookId: String) =>
            eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map { eventFacebookIds =>
              eventFacebookIds.map { eventId: String =>
                eventMethods.saveFacebookEventByFacebookId(eventId) recover {
                  case NonFatal(e) => Logger.error("Scheduler.findEventsForOrganizers: ", e)
                }
              }
            } recover {
              case NonFatal(e) => Logger.error("Scheduler.findEventsForOrganizers: ", e)
            }

          Thread.sleep(4000)
          findEventsForOrganizersOneByOne(offset + 1)

        case _ =>
      }

      case _ =>
        Logger.info("Scheduler.findEventsForOrganizersOneByOne: DONE :)")
    }
  }

  def findTracksForArtistsOneByOne(offset: Long = 0): Unit = artistMethods.findSinceOffset(numberToReturn = 1, offset) map {
    artists =>
    artists.headOption match {
      case Some(artist) =>
        val tracksEnumerator = artistMethods.getArtistTracks(PatternAndArtist(artist.artist.name, artist))
        trackMethods.saveEnumeratorWithDelay(tracksEnumerator)

        Thread.sleep(4000)
        findTracksForArtistsOneByOne(offset + 1)

      case _ =>
        Logger.info("Scheduler.findTracksForArtistsOneByOne: DONE :)")
    }
  }

  def updateGeographicPointOfPlaces50By50(offset: Long = 0): Unit = placeMethods.findSinceOffset(offset, numberToReturn = 50) map {
    places =>
      updateGeographicPointOfPlaces(places)
      places.size match {
        case 50 =>
          Thread.sleep(1500)
          updateGeographicPointOfPlaces50By50(offset + 50)
        case _ =>
      }
  }

  def updateGeographicPointOfPlaces(places: Seq[PlaceWithAddress]): Seq[Any] = places map { place =>
    place.place.geographicPoint match {
      case None =>
        place.address match {
          case Some(address) =>
            getGeoPointOfPlaceIfAbsent(place, address)
          case _ =>
        }
      case _ =>
    }
  }

  def updateGeographicPointOfOrganizers50By50(offset: Long = 0): Unit =
    organizerMethods.findSinceOffset(offset, numberToReturn = 50) map {
    organizers =>
      updateGeographicPointOfOrganizers(organizers)
      organizers.size match {
        case 50 =>
          Thread.sleep(1500)
          updateGeographicPointOfPlaces50By50(offset + 50)
        case _ =>
      }
  }

  def updateGeographicPointOfOrganizers(organizers: Seq[OrganizerWithAddress]): Seq[Any] = organizers map { organizer =>
    organizer.organizer.geographicPoint match {
      case None =>
        organizer.address match {
          case Some(address) =>
            getGeoPointOfOrganizerIfAbsent(organizer, address)
          case _ =>
        }
      case _ =>
    }
  }

  def updateGeographicPointOfEvents50By50(offset: Long = 0): Unit =
    eventMethods.findSinceOffset(offset, numberToReturn = 50) map { events =>
      updateGeographicPointOfEvents(events)

      events.size match {
        case 50 =>
          Thread.sleep(1500)
          updateGeographicPointOfEvents50By50(offset + 50)
        case _ =>
      }
  }

  def updateGeographicPointOfEvents(events: Seq[EventWithRelations]): Seq[Any] = events map { event =>
    event.event.geographicPoint match {
      case None =>
        event.addresses.headOption match {
          case Some(address) =>
            getGeoPointOfEventIfAbsent(event, address)
          case _ =>
            if (event.places.nonEmpty) {
              event.places.head.address match {
                case Some(address) =>
                  getGeoPointOfEventIfAbsent(event, address)
                case _ =>
              }
            }
        }
      case _ =>
    }
  }

  def getGeoPointOfPlaceIfAbsent(place: PlaceWithAddress, address: Address): Future[Any] = address.geographicPoint match {
    case Some(geoPoint) =>
      val updatedPlace = place.place.copy(geographicPoint = Option(geoPoint))
      placeMethods.update(updatedPlace)
    case _ =>
      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
        addressWithMaybeGeographicPoint.geographicPoint match {
          case Some(geoPoint) =>
            addressMethods.update(addressWithMaybeGeographicPoint)
            placeMethods.update(place.place.copy(geographicPoint = Option(geoPoint)));
          case _ =>
            None
        }
      } recover {
        case NonFatal(e) => Logger.error("Scheduler.getGeoPointOfPlaceIfAbsent\nMessage: " + e.getMessage)
      }
  }
  
  def getGeoPointOfOrganizerIfAbsent(organizer: OrganizerWithAddress, address: Address): Future[Any] = address.geographicPoint match {
    case Some(geoPoint) =>
      val updatedOrganizer = organizer.organizer.copy(geographicPoint = Option(geoPoint))
      organizerMethods.update(updatedOrganizer)
    case _ =>
      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
        addressWithMaybeGeographicPoint.geographicPoint match {
          case Some(geoPoint) =>
            addressMethods.update(addressWithMaybeGeographicPoint)
            organizerMethods.update(organizer.organizer.copy(geographicPoint = Option(geoPoint)));
          case _ =>
            None
        }
      } recover {
        case NonFatal(e) => Logger.error("Scheduler.getGeoPointOfOrganizerIfAbsent\nMessage: " + e.getMessage)
      }
  }
  
  def getGeoPointOfEventIfAbsent(event: EventWithRelations, address: Address): Future[Any] = address.geographicPoint match {
    case Some(geoPoint) =>
      val updatedEvent = event.event.copy(geographicPoint = Option(geoPoint))
      eventMethods.update(updatedEvent)
    case _ =>
      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
        addressWithMaybeGeographicPoint.geographicPoint match {
          case Some(geoPoint) =>
            addressMethods.update(addressWithMaybeGeographicPoint)
            eventMethods.update(event.event.copy(geographicPoint = Option(geoPoint)));
          case _ =>
            None
        }
      } recover {
        case NonFatal(e) => Logger.error("Scheduler.getGeoPointOfEventIfAbsent\nMessage: " + e.getMessage)
      }
  }
}
