package jobs

import javax.inject.Inject

import models._
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Enumeratee, Iteratee}
import play.api.Play.current
import scala.concurrent.duration._
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
      place.facebookId match {
        case Some(facebookId) =>
          Thread.sleep(400)
          eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map {
            _.map { eventId =>
              Thread.sleep(600)
              eventMethods.findEventOnFacebookByFacebookId(eventId) map {
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

  def findEventsForOrganizers(): Unit = {
    organizerMethods.findAll map {
      _ map { organizer =>
        organizer.organizer.facebookId match {
          case Some(facebookId: String) =>
            Thread.sleep(200)
            eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map { eventFacebookIds =>
              Thread.sleep(300)
              eventFacebookIds.map { eventId: String =>
                Thread.sleep(200)
                eventMethods.findEventOnFacebookByFacebookId(eventId) map {
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
  }

  def findTracksForArtists(): Unit = artistMethods.findAll map { artists =>
    artists map { artist =>
      Thread.sleep(2000)
      val tracksEnumerator = artistMethods.getArtistTracks(PatternAndArtist(artist.name, ArtistWithWeightedGenresAndHasTrack(artist)))
      val toTracksWithDelay: Enumeratee[Set[Track], Set[Track]] = Enumeratee.map[Set[Track]] { tracks: Set[Track] =>
        Thread.sleep(1000)
        tracks
      }
      tracksEnumerator |>> toTracksWithDelay &>> Iteratee.foreach { tracks =>
        trackMethods.saveSequence(tracks)
      }
    }
  }

  def updateGeographicPoints(): Unit = {
    placeMethods.findAll map { places =>
      Thread.sleep(2000)
      places map { place =>
        if (place.geographicPoint.isEmpty) {
          place.addressId match {
            case Some(addressId) =>
              addressMethods.find(addressId) map {
                case Some(address) =>
                  address.geographicPoint match {
                    case Some(geographicPoint) =>
                      placeMethods.update(place.copy(geographicPoint = Option(geographicPoint)));
                    case _ =>
                      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
                        addressMethods.update(addressWithMaybeGeographicPoint)
                        addressWithMaybeGeographicPoint.geographicPoint match {
                          case Some(geographicPoint) =>
                            placeMethods.update(place.copy(geographicPoint = Option(geographicPoint)));
                          case _ =>
                            None
                        }
                      }
                  }
                case _ =>
                  None
              }
            case _ =>
              None
          }
        }
      }
    }
    organizerMethods.findAll map { organizers =>
      Thread.sleep(2000)
      organizers map { organizer =>
        if (organizer.organizer.geographicPoint.isEmpty) {
          organizer.organizer.addressId match {
            case Some(addressId) =>
              addressMethods.find(addressId) map {
                case Some(address) =>
                  address.geographicPoint match {
                    case Some(geographicPoint) =>
                      organizerMethods.update(organizer.organizer.copy(geographicPoint = Option(geographicPoint)));
                    case _ =>
                      searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
                        addressMethods.update(addressWithMaybeGeographicPoint)
                        addressWithMaybeGeographicPoint.geographicPoint match {
                          case Some(geographicPoint) =>
                            organizerMethods.update(organizer.organizer.copy(geographicPoint = Option(geographicPoint)));
                          case _ =>
                            None
                        }
                      }
                  }
                case _ =>
                  None
              }
            case _ =>
              None
          }
        }
      }
    }
    eventMethods.findAll map { events =>
      Thread.sleep(2000)
      events map { event =>
        if (event.event.geographicPoint.isEmpty) {
          if (event.addresses.nonEmpty) {
            event.addresses.head.geographicPoint match {
              case Some(geographicPoint) =>
                eventMethods.update(event.event.copy(geographicPoint = Option(geographicPoint)));
              case _ =>
                searchGeographicPoint.getGeographicPoint(event.addresses.head, retry = 3) map { addressWithMaybeGeographicPoint =>
                  addressMethods.update(addressWithMaybeGeographicPoint)
                  addressWithMaybeGeographicPoint.geographicPoint match {
                    case Some(geographicPoint) =>
                      eventMethods.update(event.event.copy(geographicPoint = Option(geographicPoint)));
                    case _ =>
                      None
                  }
                }
            }
          } else if (event.places.nonEmpty) {
            event.places.head.address match {
              case Some(address) =>
                address.geographicPoint match {
                  case Some(geographicPoint) =>
                    eventMethods.update(event.event.copy(geographicPoint = Option(geographicPoint)));
                  case _ =>
                    searchGeographicPoint.getGeographicPoint(address, retry = 3) map { addressWithMaybeGeographicPoint =>
                      addressMethods.update(addressWithMaybeGeographicPoint)
                      addressWithMaybeGeographicPoint.geographicPoint match {
                        case Some(geographicPoint) =>
                          eventMethods.update(event.event.copy(geographicPoint = Option(geographicPoint)));
                        case _ =>
                          None
                      }
                    }
                }
              case _ =>
                None
            }
          }
        }
      }
    }
  }
}
