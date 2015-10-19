package jobs

import models._
import play.api.Logger
<<<<<<< HEAD
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.json._
=======
>>>>>>> master
import play.api.libs.concurrent.Execution.Implicits._
import javax.inject.Inject


class Scheduler @Inject()(val eventMethods: EventMethods,
                          val placeMethods: PlaceMethods) {
  def start(): Unit = {
<<<<<<< HEAD
    findEventsForPlaces()
    findEventsForOrganizers()
    findTracksForArtists()
  }
  
  def findEventsForOrganizers(): Unit = {
    Organizer.findAllWithFacebookId map {
      _ map { organizer =>
        Event.getEventsFacebookIdByPlaceOrOrganizerFacebookId(organizer.facebookId.get) map {
          _.map { eventId =>
            Event.findEventOnFacebookByFacebookId(eventId)
          }
        }
        
      }
    }
  }

  def findEventsForPlaces(): Unit = Place.findAllWithFacebookId map {
    _ map { place =>
      Event.getEventsFacebookIdByPlaceOrOrganizerFacebookId(place.facebookId.get) map {
        _.map { eventId =>
          Event.findEventOnFacebookByFacebookId(eventId) map {
            saveEventWithGeographicPointAndPlaceRelation(_, place.placeId.get, place.geographicPoint)
          }
        }
      }
    }
  }

  def findTracksForArtists(): Unit = Artist.findAll map { artist =>
    Artist.getArtistTracks(Artist.PatternAndArtist(artist.name, artist)) |>> Iteratee.foreach{ a =>
      a.map { Track.save }
  }
  }

  def saveEventWithGeographicPointAndPlaceRelation(facebookEvent: Event, placeId: Long,
                                                   placeGeographicPoint: Option[String]): Unit = {
    Event.save(facebookEvent.copy(geographicPoint = placeGeographicPoint)) match {
      case Some(eventId) =>
        Place.saveEventRelation(eventId, placeId)
      case _ =>
        Logger.error("Scheduler.saveEventWithGeographicPointAndPlaceRelation: event not saved")
    }
  }
=======
//    findEventsForPlaces()
//    findEventsForOrganizers()
//    findTracksForArtists()
  }

//  def findEventsForOrganizers(): Unit = {
//    Organizer.findAllWithFacebookId map {
//      _ map { organizer =>
//        Event.getEventsFacebookIdByPlaceOrOrganizerFacebookId(organizer.facebookId.get) map {
//          _.map { eventId =>
//            Event.findEventOnFacebookByFacebookId(eventId)
//          }
//        }
//
//      }
//    }
//  }
//
//  def findEventsForPlaces(): Unit = Place.findAllWithFacebookId map {
//    _ map { place =>
//      Event.getEventsFacebookIdByPlaceOrOrganizerFacebookId(place.facebookId.get) map {
//        _.map { eventId =>
//          Event.findEventOnFacebookByFacebookId(eventId) map {
//            saveEventWithGeographicPointAndPlaceRelation(_, place.placeId.get, place.geographicPoint)
//          }
//        }
//      }
//    }
//  }
//
//  def findTracksForArtists(): Unit = Artist.findAll map { artist =>
//    Artist.getArtistTracks(Artist.PatternAndArtist(artist.name, artist)) |>> Iteratee.foreach{ tracks =>
//      tracks.map { Track.save }
//    }
//  }

//  def start(): Unit = placeMethods.findAllWithFacebookId map { _ map { place =>
//    eventMethods.getEventsFacebookIdByPlace(place.facebookId.get) map { _.map { eventId =>
//      eventMethods.findEventOnFacebookByFacebookId(eventId) map {
//        saveEventWithGeographicPointAndPlaceRelation(_, place.id.get, place.geographicPoint)
//      }
//    }}
//  }}
//
//  def saveEventWithGeographicPointAndPlaceRelation(facebookEvent: Event, placeId: Long,
//                                                   placeGeographicPoint: Option[String]): Unit = {
//    eventMethods.save(facebookEvent.copy(geographicPoint = placeGeographicPoint)) match {
//      case Some(eventId) =>
//        placeMethods.saveEventRelation(eventId, placeId)
//      case _ =>
//        Logger.error("Scheduler.saveEventWithGeographicPointAndPlaceRelation: event not saved")
//    }
//  }
>>>>>>> master
}
