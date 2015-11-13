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
                          val placeMethods: PlaceMethods) {

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
}
