package services

import java.util.UUID

import services.MyPostgresDriver.api._
import controllers.ThereIsNoArtistForThisFacebookIdException
import models._
import play.api.Logger
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import silhouette.DBTableDefinitions

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait FollowService extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {
  
  //////////////////////////////////////////// artist ///////////////////////////////////////////////////////////////
  
  def followByArtistId(userArtistRelation: UserArtistRelation): Future[Int] = db.run(artistsFollowed += userArtistRelation)

  def unfollowByArtistId(userArtistRelation: UserArtistRelation): Future[Int] = db.run(
    artistsFollowed
      .filter(artistFollowed =>
        artistFollowed.userId === userArtistRelation.userId && artistFollowed.artistId === userArtistRelation.artistId)
      .delete)
  

  def getFollowedArtists(userId: UUID): Future[Seq[Artist] ]= {
    val query = for {
      artistFollowed <- artistsFollowed if artistFollowed.userId === userId
      artist <- artists if artist.id === artistFollowed.artistId
    } yield artist

    db.run(query.result)
  }

  def isFollowed(userArtistRelation: UserArtistRelation): Future[Boolean] = {
    val query =
      sql"""SELECT exists(SELECT 1 FROM artistsFollowed WHERE userId = ${userArtistRelation.userId}
           AND artistId = ${userArtistRelation.artistId})"""
        .as[Boolean]
    db.run(query.head)
  }

  //////////////////////////////////////////// event ///////////////////////////////////////////////////////////////

  def follow(userEventRelation: UserEventRelation): Future[Int] = db.run(eventsFollowed += userEventRelation)

  def unfollow(userEventRelation: UserEventRelation): Future[Int] = db.run(
    eventsFollowed
      .filter(eventFollowed =>
        eventFollowed.userId === userEventRelation.userId && eventFollowed.eventId === userEventRelation.eventId)
      .delete)

  def getFollowedEvents(userId: UUID): Future[Seq[Event] ]= {
    val query = for {
      eventFollowed <- eventsFollowed if eventFollowed.userId === userId
      event <- events if event.id === eventFollowed.eventId
    } yield event

    db.run(query.result)
  }

  def isFollowed(userEventRelation: UserEventRelation): Future[Boolean] = {
    val query =
      sql"""SELECT exists(
             SELECT 1 FROM eventsFollowed WHERE userId = ${userEventRelation.userId} AND eventId = ${userEventRelation.eventId})"""
        .as[Boolean]
    db.run(query.head)
  }

  //////////////////////////////////////////// genre ///////////////////////////////////////////////////////////////

  def follow(userGenreRelation: UserGenreRelation): Future[Int] =
    db.run(genresFollowed += userGenreRelation)

  def unfollow(userGenreRelation: UserGenreRelation): Future[Int] = db.run(
    genresFollowed
      .filter(genreFollowed =>
        genreFollowed.userId === userGenreRelation.userId && genreFollowed.genreId === userGenreRelation.genreId)
      .delete)

  def getFollowedGenres(userId: UUID): Future[Seq[Genre] ]= {
    val query = for {
      genreFollowed <- genresFollowed if genreFollowed.userId === userId
      genre <- genres if genre.id === genreFollowed.genreId
    } yield genre

    db.run(query.result)
  }

//  def isFollowed(userGenreRelation: UserGenreRelation): Future[Boolean] = {
//    val query =
//      sql"""SELECT exists(
//             SELECT 1 FROM genresFollowed WHERE userId = ${userGenreRelation.userId} AND genreId = ${userGenreRelation.genreId})"""
//        .as[Boolean]
//    db.run(query.head)
//  }

  //////////////////////////////////////////// organizer ///////////////////////////////////////////////////////////////

  def followByOrganizerId(userOrganizerRelation: UserOrganizerRelation): Future[Int] =
    db.run(organizersFollowed += userOrganizerRelation)

  def unfollow(userOrganizerRelation: UserOrganizerRelation): Future[Int] = db.run(
    organizersFollowed
      .filter(organizerFollowed =>
        organizerFollowed.userId === userOrganizerRelation.userId && organizerFollowed.organizerId === userOrganizerRelation.organizerId)
      .delete)

  def isFollowed(userOrganizerRelation: UserOrganizerRelation): Future[Boolean] = {
    val query =
      sql"""SELECT exists(
             SELECT 1 FROM organizersFollowed WHERE userId = ${userOrganizerRelation.userId} AND organizerId = ${userOrganizerRelation.organizerId})"""
        .as[Boolean]
    db.run(query.head)
  }

  def getFollowedOrganizers(userId: UUID): Future[Seq[OrganizerWithAddress]]= {
    val query = for {
      organizerFollowed <- organizersFollowed if organizerFollowed.userId === userId
      organizerWithAddress <- organizers joinLeft addresses on (_.addressId === _.id)
      if organizerWithAddress._1.id === organizerFollowed.organizerId
    } yield organizerWithAddress

    db.run(query.result) map(_ map OrganizerWithAddress.tupled)
  }

  //////////////////////////////////////////// place ///////////////////////////////////////////////////////////////

  def followByPlaceId(placeRelation: UserPlaceRelation): Future[Int] = db.run(placesFollowed += placeRelation)

  def unfollow(userPlaceRelation: UserPlaceRelation): Future[Int] = db.run(
    placesFollowed
      .filter(placeFollowed =>
        placeFollowed.userId === userPlaceRelation.userId && placeFollowed.placeId === userPlaceRelation.placeId)
      .delete)
  

  def isFollowed(userPlaceRelation: UserPlaceRelation): Future[Boolean] = {
    val query =
      sql"""SELECT exists(
           SELECT 1 FROM placesFollowed WHERE userId = ${userPlaceRelation.userId} AND placeId = ${userPlaceRelation.placeId})"""
        .as[Boolean]
    db.run(query.head)
  }

  def getFollowedPlaces(userId: UUID): Future[Seq[Place] ]= {
    val query = for {
      placeFollowed <- placesFollowed if placeFollowed.userId === userId
      place <- places if place.id === placeFollowed.placeId
    } yield place

    db.run(query.result)
  }

  //////////////////////////////////////////// track ///////////////////////////////////////////////////////////////

  def followByTrackId(trackRelation: UserTrackRelation): Future[Int] = db.run(tracksFollowed += trackRelation)

  def unfollowByTrackId(userTrackRelation: UserTrackRelation): Future[Int] = db.run(
    tracksFollowed
      .filter(trackFollowed =>
        trackFollowed.userId === userTrackRelation.userId && trackFollowed.trackId === userTrackRelation.trackId)
      .delete)


  def isFollowed(userTrackRelation: UserTrackRelation): Future[Boolean] = {
    val query =
      sql"""SELECT exists(
           SELECT 1 FROM usersFavoriteTracks WHERE userId = ${userTrackRelation.userId} AND trackId = ${userTrackRelation.trackId})"""
        .as[Boolean]
    db.run(query.head)
  }

  def getFollowedTracks(userId: UUID): Future[Seq[Track] ]= {
    val query = for {
      trackFollowed <- tracksFollowed if trackFollowed.userId === userId
      track <- tracks if track.uuid === trackFollowed.trackId
    } yield track

    db.run(query.result)
  }
}