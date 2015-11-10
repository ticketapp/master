package jobs

import javax.inject.Inject

import models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Iteratee

import scala.util.control.NonFatal


class Scheduler @Inject()(val eventMethods: EventMethods,
                          val organizerMethods: OrganizerMethods,
                          val artistMethods: ArtistMethods,
                          val trackMethods: TrackMethods,
                          val placeMethods: PlaceMethods) {
  def start(): Unit = {
    findEventsForPlaces()
//    findEventsForOrganizers()
//    findTracksForArtists()
  }

  def findEventsForOrganizers(): Unit = {
    organizerMethods.findAll map {
      _ map { organizer =>
        organizer.organizer.facebookId match {
          case Some(facebookId) =>
            eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map { eventFacebookIds =>
              eventFacebookIds.map { eventId: String =>
                Thread.sleep(200)
                eventMethods.findEventOnFacebookByFacebookId(eventId) map {
                  eventMethods.save
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

  def findEventsForPlaces(): Unit = placeMethods.findAll map {
    _ map { place =>
      place.facebookId match {
        case Some(facebookId) =>
          eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map {
            _.map { eventId =>
              Thread.sleep(200)
              eventMethods.findEventOnFacebookByFacebookId(eventId) map {
                Thread.sleep(100)
                eventMethods.save
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

  def findTracksForArtists(): Unit = artistMethods.findAll map { artists =>
    artists map { artist =>
      artistMethods.getArtistTracks(PatternAndArtist(artist.name, ArtistWithWeightedGenres(artist))) |>> Iteratee.foreach{ tracks =>
        tracks.map { trackMethods.save }
      }
    }
  }
}
