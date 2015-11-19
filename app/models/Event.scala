package models

import javax.inject.Inject

import com.vividsolutions.jts.geom.Geometry
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{FollowService, MyPostgresDriver, Utilities}
import silhouette.DBTableDefinitions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}


case class Event(id: Option[Long],
                 facebookId: Option[String],
                 isPublic: Boolean,
                 isActive: Boolean,
                 name: String,
                 geographicPoint: Option[Geometry],
                 description: Option[String],
                 startTime: DateTime,
                 endTime: Option[DateTime],
                 ageRestriction: Int = 16,
                 tariffRange: Option[String] = None,
                 ticketSellers: Option[String] = None,
                 imagePath: Option[String])

case class EventWithRelations(event: Event,
                              organizers: Seq[OrganizerWithAddress] = Vector.empty,
                              artists: Seq[ArtistWithWeightedGenres] = Vector.empty,
                              places: Seq[PlaceWithAddress] = Vector.empty,
                              genres: Seq[Genre] = Vector.empty,
                              addresses: Seq[Address] = Vector.empty)


class EventMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val artistMethods: ArtistMethods,
                             val tariffMethods: TariffMethods,
                             val trackMethods: TrackMethods,
                             val genreMethods: GenreMethods,
                             val placeMethods: PlaceMethods,
                             val geographicPointMethods: SearchGeographicPoint,
                             val addressMethods: AddressMethods,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with FollowService
    with DBTableDefinitions
    with eventWithRelationsTupleToEventWithRelationsClass
    with MyDBTableDefinitions {

  def findAll(): Future[Seq[EventWithRelations]] = {
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events joinLeft
        (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
        (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
        (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
        (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
        (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def find(id: Long): Future[Option[EventWithRelations]] = {
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
          .filter(_.id === id) joinLeft
        (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
        (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
        (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
        (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
        (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations)) map {
      _.headOption
    }
  }



  def sortEventWithRelationsNearPoint(geographicPoint: Geometry, eventsWihRelations: Vector[EventWithRelations]): Vector[EventWithRelations] = {
    val eventsWihRelationsWithoutGeoPoint = eventsWihRelations
      .filter(_.event.geographicPoint.isEmpty)
    val eventsWihRelationsSortedByGeoPoint = eventsWihRelations
      .filter(_.event.geographicPoint.nonEmpty)
      .sortBy(event => geographicPoint.distance(event.event.geographicPoint.get))
    eventsWihRelationsSortedByGeoPoint ++ eventsWihRelationsWithoutGeoPoint
  }

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(event => event.endTime.nonEmpty && event.endTime > now ||
          event.endTime.isEmpty && event.startTime > twelveHoursAgo)
        .sortBy(_.geographicPoint <-> geographicPoint)
        .drop(offset)
        .take(numberToReturn) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
      optionalEventAddresses)

    db.run(query.result) map(eventWithRelations =>
      sortEventWithRelationsNearPoint(geographicPoint, eventWithRelationsTupleToEventWithRelationClass(eventWithRelations)))
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[EventWithRelations]] = geographicPointMethods
    .findGeographicPointOfCity(city) flatMap {
    case None => Future { Seq.empty }
    case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
  }

  def findInPeriodNear(hourInterval: Int, geographicPoint: Geometry, offset: Int, numberToReturn: Int)
  : Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val xHoursLater = now.plusHours(hourInterval)
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(event =>
          (event.endTime.nonEmpty && event.endTime < xHoursLater && event.endTime > now) ||
            (event.endTime.isEmpty && event.startTime < xHoursLater && event.startTime >= twelveHoursAgo ))
        .sortBy(_.geographicPoint <-> geographicPoint)
        .drop(offset)
        .take(numberToReturn) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations =>
      sortEventWithRelationsNearPoint(geographicPoint, eventWithRelationsTupleToEventWithRelationClass(eventWithRelations)))
  }

  def findPassedInHourIntervalNear(hourInterval: Int, geographicPoint: Geometry, offset: Int, numberToReturn: Int)
  : Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val xHoursAgo = now.minusHours(hourInterval)

    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(event => (event.startTime < now) && (event.startTime > xHoursAgo))
        .sortBy(_.geographicPoint <-> geographicPoint)
        .drop(offset)
        .take(numberToReturn) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllByGenre(genreName: String, geographicPoint: Geometry, offset: Int, numberToReturn: Int)
  : Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      genre <- genres if genre.name === genreName
      eventGenre <- eventsGenres if eventGenre.genreId === genre.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(event => event.endTime.nonEmpty && event.endTime > now ||
          event.endTime.isEmpty && event.startTime > twelveHoursAgo)
        .sortBy(_.geographicPoint <-> geographicPoint)
        .drop(offset)
        .take(numberToReturn) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventGenre.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations =>
      sortEventWithRelationsNearPoint(geographicPoint, eventWithRelationsTupleToEventWithRelationClass(eventWithRelations)))
  }

  def findAllByPlace(placeId: Long): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      place <- places if place.id === placeId
      eventPlace <- eventsPlaces if eventPlace.placeId === place.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(event => ((event.endTime.nonEmpty && event.endTime > now)
          || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventPlace.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations =>
      eventWithRelationsTupleToEventWithRelationClass(eventWithRelations).sortBy(-_.event.startTime.getMillis))
  }


  def findAllPassedByPlace(placeId: Long): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()

    val query = for {
      place <- places if place.id === placeId
      eventPlace <- eventsPlaces if eventPlace.placeId === place.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(_.startTime < now)
        .sortBy(_.startTime.desc) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventPlace.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllByOrganizer(organizerId: Long): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      organizer <- organizers if organizer.id === organizerId
      eventOrganizer <- eventsOrganizers if eventOrganizer.organizerId === organizer.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(event => ((event.endTime.nonEmpty && event.endTime > now)
          || (event.endTime.isEmpty && event.startTime > twelveHoursAgo)))
        .sortBy(_.startTime.desc) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventOrganizer.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllPassedByOrganizer(organizerId: Long): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()

    val query = for {
      organizer <- organizers if organizer.id === organizerId
      eventOrganizer <- eventsOrganizers if eventOrganizer.organizerId === organizer.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(_.startTime < now)
        .sortBy(_.startTime.desc) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventOrganizer.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllByArtist(facebookUrl: String): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      artist <- artists if artist.facebookUrl === facebookUrl
      eventArtist <- eventsArtists if eventArtist.artistId === artist.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(event => (event.endTime.nonEmpty && event.endTime > now) ||
          (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
        .sortBy(_.startTime.desc) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventArtist.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllPassedByArtist(artistId: Long): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()

    val query = for {
      artist <- artists
      if artist.id === artistId
      eventArtist <- eventsArtists if eventArtist.artistId === artist.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(_.startTime < now)
        .sortBy(_.startTime.desc) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventArtist.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllContaining(pattern: String): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(event =>
          (event.name.toLowerCase like s"%$lowercasePattern%") &&
          ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo)))
        .sortBy(_.startTime.desc) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllContaining(pattern: String, geographicPoint: Geometry): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(event => (event.name.toLowerCase like s"%$lowercasePattern%") &&
          ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo)))
        .sortBy(_.geographicPoint <-> geographicPoint) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def findAllByCityPattern(cityPattern: String): Future[Seq[EventWithRelations]] = {
    val lowercasePattern = cityPattern.toLowerCase

    val query = for {
      address <- addresses.filter(_.city.toLowerCase like s"%$lowercasePattern%")
      eventAddress <- eventsAddresses if eventAddress.addressId === address.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if  eventWithOptionalEventOrganizers._1.id === eventAddress.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelationClass(eventWithRelations))
  }

  def save(eventWithRelations: EventWithRelations): Future[Event] = db.run((for {
    eventFound <- events.filter(_.facebookId === eventWithRelations.event.facebookId).result.headOption
    result <- eventFound.map(DBIO.successful).getOrElse(events returning events.map(_.id) += eventWithRelations.event)
  } yield result match {
    case e: Event =>
      saveEventRelations(eventWithRelations, e.id.get)
      e
    case id: Long =>
      saveEventRelations(eventWithRelations, id)
      eventWithRelations.event.copy(id = Option(id))
  }).transactionally)

  def saveEventRelations(eventWithRelations: EventWithRelations, eventId: Long): Future[Any] = Future {
    eventWithRelations.genres map(genre => genreMethods.saveWithEventRelation(genre, eventId))
    eventWithRelations.artists map(artist => artistMethods.saveWithEventRelation(artist, eventId))
    eventWithRelations.organizers map(organizer => organizerMethods.saveWithEventRelation(organizer, eventId))
    eventWithRelations.places map(place => placeMethods.saveWithEventRelation(place, eventId))
    eventWithRelations.addresses map(address => addressMethods.saveWithEventRelation(address, eventId))
  }

  def delete(id: Long): Future[Int] = db.run(events.filter(_.id === id).delete)

  def update(event: Event): Future[Int] = db.run(events.filter(_.id === event.id).update(event))

  def saveFacebookEventByFacebookId(eventFacebookId: String): Future[Option[Event]] = {
    val maybeEvent = db.run(events.filter(_.facebookId === eventFacebookId).result.headOption)
    maybeEvent flatMap {
      case Some(event) =>
        Future(Option(event))
      case None =>
        getEventOnFacebookByFacebookId(eventFacebookId) flatMap {
          case Some(event) =>
            Future {
              event.artists.foreach { artist =>
              val tracksEnumerator = artistMethods.getArtistTracks(PatternAndArtist(artist.artist.name, artist))
              tracksEnumerator |>> Iteratee.foreach(tracks => trackMethods.saveSequence(tracks))
                artist
              }
            }
            save(event) map Option.apply
          case None =>
            Future(None)
        }
    }
  }

  def getEventOnFacebookByFacebookId(eventFacebookId: String): Future[Option[EventWithRelations]] =
    WS.url("https://graph.facebook.com/" + utilities.facebookApiVersion + "/" + eventFacebookId)
      .withQueryString(
        "fields" -> "cover,description,name,start_time,end_time,owner,venue,place",
        "access_token" -> utilities.facebookToken)
      .get()
      .flatMap(facebookEventToEventWithRelations) recover {
      case NonFatal(e) =>
        Logger.error("Event.findEventOnFacebookByFacebookId:\nMessage:\n", e)
        None
    }

  case class MaybeOwnerAndPlaceIds(maybeOwnerId: Option[String], maybePlaceId: Option[String])

  def readFacebookEvent: Reads[(EventWithRelations, MaybeOwnerAndPlaceIds)] = (
    (__ \ "description").readNullable[String] and
      (__ \ "cover").readNullable[String](
        (__ \ "source").read[String]
      ) and
      (__ \ "name").read[String] and
      (__ \ "id").readNullable[String] and
      (__ \ "start_time").read[String] and
      (__ \ "endTime").readNullable[String] and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "street").readNullable[String]
      ) and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "zip").readNullable[String]
      ) and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "city").readNullable[String]
      ) and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "country").readNullable[String]
      ) and
      (__ \ "owner").readNullable[Option[String]](
        (__ \ "id").readNullable[String]
      ) and
      (__ \ "place").readNullable[Option[String]](
        (__ \ "id").readNullable[String]
      )
    )((maybeDescription: Option[String], maybeCover: Option[String], name: String, facebookId: Option[String],
       startTime: String, endTime: Option[String], street: Option[Option[String]], zip: Option[Option[String]],
       city: Option[Option[String]], country: Option[Option[String]], maybeOwnerId: Option[Option[String]],
       maybePlaceId: Option[Option[String]]) => {

    val event = Event(
      id = None,
      facebookId = facebookId,
      isPublic = true,
      isActive = true,
      name = utilities.refactorEventOrPlaceName(name),
      geographicPoint = None,
      description = utilities.formatDescription(maybeDescription),
      startTime = utilities.stringToDateTime(startTime),
      endTime = utilities.optionStringToOptionDateTime(endTime),
      imagePath = maybeCover,
      tariffRange = tariffMethods.findPrices(maybeDescription))

    val addresses = Try(Address(
      id = None, geographicPoint = None,
      street = street.flatten,
      zip = zip.flatten,
      city = city.flatten)) match {
      case Success(address) => Seq(address)
      case _ => Seq.empty
    }

    val eventWithRelations = EventWithRelations(
      event = event,
      addresses = addresses)

    val maybeOwnerIdAndMaybePlaceId = MaybeOwnerAndPlaceIds(maybeOwnerId.flatten, maybePlaceId.flatten)

    (eventWithRelations, maybeOwnerIdAndMaybePlaceId)
  })

  def facebookEventToEventWithRelations(eventFacebookResponse: WSResponse): Future[Option[EventWithRelations]] = {
    Try(eventFacebookResponse.json.as[(EventWithRelations, MaybeOwnerAndPlaceIds)](readFacebookEvent)) match {
      case Success(eventWithRelationsAndMaybePlaceAndOwnerIds) =>
        val eventWithRelations = eventWithRelationsAndMaybePlaceAndOwnerIds._1

        val eventuallyMaybeOrganizer = organizerMethods.getOrganizerInfo(eventWithRelationsAndMaybePlaceAndOwnerIds._2.maybeOwnerId)

        val eventuallyMaybePlace: Future[Option[PlaceWithAddress]] = eventWithRelationsAndMaybePlaceAndOwnerIds._2.maybePlaceId match {
          case Some(placeId) => placeMethods.getPlaceByFacebookId(placeId)
          case None => Future(None)
        }

        val eventuallyNormalizedWebsites: Future[Set[String]] = eventWithRelations.event.description match {
          case Some(description) => utilities.getNormalizedWebsitesInText(description)
          case None => Future(Set.empty)
        }

        for {
          organizer <- eventuallyMaybeOrganizer
          normalizedWebsites <- eventuallyNormalizedWebsites
          artistsFromDescription <- artistMethods.getFacebookArtistsByWebsites(normalizedWebsites)
          artistsFromTitle <- artistMethods.getEventuallyArtistsInEventTitle(
            eventWithRelationsAndMaybePlaceAndOwnerIds._1.event.name, normalizedWebsites)
          maybePlace <- eventuallyMaybePlace
          nonEmptyArtists = (artistsFromDescription.toVector ++ artistsFromTitle).distinct
          artistGenres = nonEmptyArtists.flatMap(_.genres map (_.genre))
          overGenres <-genreMethods.findOverGenres(artistGenres)
          genres = (artistGenres ++ overGenres).distinct
        } yield {

          val ticketSellers = tariffMethods.findTicketSellers(normalizedWebsites)

          Option(eventWithRelations.copy(
            event = eventWithRelations.event.copy(ticketSellers = ticketSellers),
            artists = nonEmptyArtists,
            organizers = Vector(organizer).flatten,
            places = Vector(maybePlace).flatten,
            genres = genres))
        }

      case Failure(t: Throwable) =>
        Logger.error("Event.facebookEventToEventWithRelations: from readFacebookEvent:\nMessage:\n" + t.getMessage +
          "\nFor facebook response:\n" + eventFacebookResponse.json)
        Future(None)
    }
  }

  def getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId: String): Future[Seq[String]] = {
    WS.url("https://graph.facebook.com/" + utilities.facebookApiVersion + "/" + facebookId + "/events/")
      .withQueryString("access_token" -> utilities.facebookToken)
      .get()
      .map(readEventsIdsFromWSResponse)
      .recover {
        case NonFatal(e) =>
          Logger.error("Event.getEventsFacebookIdByPlaceOrOrganizerFacebookId:\nMessage:\n", e)
          Seq.empty
      }
  }

  def readEventsIdsFromWSResponse(resp: WSResponse): Seq[String] = Try {
    val readFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readFacebookIds).flatten
  } match {
    case Success(facebookIds) => facebookIds
    case _ => Seq.empty
  }
}