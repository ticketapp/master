package jobs

import javax.inject.Inject

import models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Enumeratee, Iteratee}

import scala.concurrent.Future
import scala.util.control.NonFatal

class Scheduler @Inject()(val eventMethods: EventMethods,
                          val organizerMethods: OrganizerMethods,
                          val artistMethods: ArtistMethods,
                          val trackMethods: TrackMethods,
                          val placeMethods: PlaceMethods,
                          val addressMethods: AddressMethods,
                          val searchGeographicPoint: SearchGeographicPoint) {

  def findEventsForPlaces(): Unit = placeMethods.findAll map {
    _ map { place =>
      place.place.facebookId match {
        case Some(facebookId) =>
          Thread.sleep(400)
          eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map {
            _.map { eventId =>
              Thread.sleep(600)
              eventMethods.getEventOnFacebookByFacebookId(eventId) map {
                case Some(event) =>
                  Thread.sleep(200)
                  eventMethods.save(event)
                case _ =>
                  None
              } recover {
                case NonFatal(e) => Logger.error("Scheduler.findEventsForPlaces: ", e)
              }
            }
          } recover {
            case NonFatal(e) => Logger.error("Scheduler.findEventsForPlaces: ", e)
          }
        case None =>
      }
    }
  }

  def findEventsForOrganizers(): Unit = organizerMethods.findAll map {
    _ map { organizer =>
      organizer.organizer.facebookId match {
        case Some(facebookId: String) =>
          Thread.sleep(200)
          eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map { eventFacebookIds =>
            Thread.sleep(300)
            eventFacebookIds.map { eventId: String =>
              Thread.sleep(200)
              eventMethods.getEventOnFacebookByFacebookId(eventId) map {
                case Some(event) => eventMethods.save(event)
                case _ =>
              } recover {
                case NonFatal(e) => Logger.error("Scheduler.findEventsForOrganizers: ", e)
              }
            }
          } recover {
            case NonFatal(e) => Logger.error("Scheduler.findEventsForOrganizers: ", e)
          }

        case None =>
      }
    }
  }

  def findTracksForArtists(): Unit = artistMethods.findAll map { artists =>
    artists map { artist =>
      Thread.sleep(2000)
      val tracksEnumerator = artistMethods.getArtistTracks(PatternAndArtist(artist.name, ArtistWithWeightedGenres(artist)))
      val toTracksWithDelay: Enumeratee[Set[Track], Set[Track]] = Enumeratee.map[Set[Track]] { tracks: Set[Track] =>
        Thread.sleep(1000)
        tracks
      }
      tracksEnumerator |>> toTracksWithDelay &>> Iteratee.foreach { tracks =>
        trackMethods.saveSequence(tracks)
      }
    }
  }

  def updateGeographicPointOfPlaces(): Unit = placeMethods.findAll map { places =>
    places map { place =>
      Thread.sleep(500)
      place.place.geographicPoint match {
        case Some(_) =>
        case _ =>
          place.address match {
            case Some(address) =>
              getGeoPointOfPlaceIfAbsent(place, address)
            case _ =>
          }
      }
    }
  }
  
  def updateGeographicPointOfOrganizers(): Unit = organizerMethods.findAll map { organizers =>
    organizers map { organizer =>
      Thread.sleep(2000)
      organizer.organizer.geographicPoint match {
        case Some(_) =>
        case _ =>
          organizer.address match {
            case Some(address) =>
              getGeoPointOfOrganizerIfAbsent(organizer, address)
            case _ =>
          }
      }
    }
  }
  
  def updateGeographicPointOfEvents(): Unit = eventMethods.findAll map { events =>
    events map { event =>
      Thread.sleep(2000)
      event.event.geographicPoint match {
        case Some(_) =>
        case _ =>
          event.addresses.headOption match {
            case Some(address) =>
              getGeoPointOfEventIfAbsent(event, address)
            case _ =>
              if (event.places.nonEmpty) {
                event.places.head.address match {
                  case Some(address) =>
                    getGeoPointOfEventIfAbsent(event, address)
                  case _ =>
                    None
                }
              }
          }
      }
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
