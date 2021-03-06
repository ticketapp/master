package services

import java.util.UUID

import application._
import artistsDomain.{artistsAndOptionalGenresToArtistsWithWeightedGenresTrait, ArtistWithWeightedGenres}
import database._
import eventsDomain.{EventWithRelations, eventWithRelationsTupleToEventWithRelations}
import genresDomain.Genre
import models._
import organizersDomain.OrganizerWithAddress
import placesDomain.PlaceWithAddress
import play.api.db.slick.HasDatabaseConfigProvider
import MyPostgresDriver.api._
import tracksDomain.{TrackTransformTrait, TrackWithGenres}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait FollowService extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with TrackTransformTrait
    with eventWithRelationsTupleToEventWithRelations
    with artistsAndOptionalGenresToArtistsWithWeightedGenresTrait {
  
  //////////////////////////////////////////// artist ///////////////////////////////////////////////////////////////
  
  def followByArtistId(userArtistRelation: UserArtistRelation): Future[Int] = db.run(artistsFollowed += userArtistRelation)

  def unfollowByArtistId(userArtistRelation: UserArtistRelation): Future[Int] = db.run(
    artistsFollowed
      .filter(artistFollowed =>
        artistFollowed.userId === userArtistRelation.userId && artistFollowed.artistId === userArtistRelation.artistId)
      .delete)


  def findFollowedArtists(userId: UUID): Future[Seq[ArtistWithWeightedGenres]]= {
    val query = for {
      artistFollowed <- artistsFollowed if artistFollowed.userId === userId
      artist <- artists joinLeft
        (artistsGenres join genres on (_.genreId === _.id)) on (_.id === _._1.artistId)
      if artist._1.id === artistFollowed.artistId
    } yield artist

    db.run(query.result) map { seqArtistAndOptionalGenre =>
      artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre)
    } map(_.toVector)
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

  def findFollowedEvents(userId: UUID): Future[Seq[EventWithRelations] ]= {
    val query = for {
      eventFollowed <- eventsFollowed if eventFollowed.userId === userId
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events joinLeft
        (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
        (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
        (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
        (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
        (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
        if ((((eventWithOptionalEventOrganizers, optionalEventArtists), optionalEventPlaces), optionalEventGenres),
          optionalEventAddresses)._1._1._1._1._1.id === eventFollowed.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
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

  def findFollowedGenres(userId: UUID): Future[Seq[Genre] ]= {
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

  def findFollowedOrganizers(userId: UUID): Future[Seq[OrganizerWithAddress]]= {
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

  def findFollowedPlaces(userId: UUID): Future[Seq[PlaceWithAddress]]= {
    val query = for {
      placeFollowed <- placesFollowed if placeFollowed.userId === userId
      place <- places joinLeft addresses on (_.addressId === _.id) if place._1.id === placeFollowed.placeId
    } yield place

    db.run(query.result) map(_ map PlaceWithAddress.tupled)
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
           SELECT 1 FROM tracksFollowed WHERE userId = ${userTrackRelation.userId} AND trackId = ${userTrackRelation.trackId})"""
        .as[Boolean]
    db.run(query.head)
  }

  def findFollowedTracks(userId: UUID): Future[Seq[TrackWithGenres]]= {
    val query = for {
      trackFollowed <- tracksFollowed if trackFollowed.userId === userId
      (track, genre) <- tracks joinLeft
        (artists join artistsGenres on (_.id === _.artistId) join genres on (_._2.genreId === _.id)) on
          (_.artistFacebookUrl === _._1._1.facebookUrl)

      if track.uuid === trackFollowed.trackId
    } yield (track, genre)

    db.run(query.result) map makeTrackWithGenres
  }
}