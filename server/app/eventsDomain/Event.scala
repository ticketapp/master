package eventsDomain

import javax.inject.Inject

import addresses.{Address, AddressMethods, SearchGeographicPoint}
import artistsDomain.{ArtistMethods, ArtistWithWeightedGenres, PatternAndArtist}
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import genresDomain.{Genre, GenreMethods}
import org.joda.time.DateTime
import organizersDomain.{OrganizerMethods, OrganizerWithAddress}
import placesDomain.{PlaceMethods, PlaceWithAddress}
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.{FollowService, SortByDistanceToPoint, SortableByGeographicPoint, Utilities}
import silhouette.DBTableDefinitions
import tariffsDomain.TariffMethods
import tracksDomain.TrackMethods

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class Event(id: Option[Long] = None,
                 facebookId: Option[String] = None,
                 isPublic: Boolean = true,
                 isActive: Boolean = false,
                 name: String,
                 geographicPoint: Geometry = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
                 description: Option[String] = None,
                 startTime: DateTime,
                 endTime: Option[DateTime] = None,
                 ageRestriction: Int = 16,
                 tariffRange: Option[String] = None,
                 ticketSellers: Option[String] = None,
                 imagePath: Option[String] = None)

case class EventWithRelations(event: Event,
                              organizers: Seq[OrganizerWithAddress] = Vector.empty,
                              artists: Seq[ArtistWithWeightedGenres] = Vector.empty,
                              places: Seq[PlaceWithAddress] = Vector.empty,
                              genres: Seq[Genre] = Vector.empty,
                              addresses: Seq[Address] = Vector.empty) extends SortableByGeographicPoint with Utilities {

  private def returnEventGeographicPointInRelations(event: Event, addresses: Seq[Address],
                                                    places: Seq[PlaceWithAddress]): Geometry =
    event.geographicPoint match {
      case notAntarcticPoint if notAntarcticPoint != antarcticPoint =>
        notAntarcticPoint

      case _ =>
        val addressesGeoPoints = addresses map(_.geographicPoint)
        val placesGeoPoint = places.map(_.geographicPoint)
        val geoPoints = addressesGeoPoints ++ placesGeoPoint

        geoPoints find(_ != antarcticPoint) match {
          case Some(geoPoint) => geoPoint
          case _ => antarcticPoint
        }
    }

  val geographicPoint: Geometry = returnEventGeographicPointInRelations(event, addresses, places)
}


class EventMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val artistMethods: ArtistMethods,
                             val tariffMethods: TariffMethods,
                             val trackMethods: TrackMethods,
                             val genreMethods: GenreMethods,
                             val placeMethods: PlaceMethods,
                             val geographicPointMethods: SearchGeographicPoint,
                             val addressMethods: AddressMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with FollowService
    with DBTableDefinitions
    with eventWithRelationsTupleToEventWithRelations
    with MyDBTableDefinitions
    with Utilities
    with SortByDistanceToPoint {

  def findSinceOffset(offset: Long, numberToReturn: Long): Future[Seq[EventWithRelations]] = {
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events.drop(offset).take(numberToReturn) joinLeft
        (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
        (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
        (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
        (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
        (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
  }

  def find(id: Long): Future[Option[EventWithRelations]] = {
    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events.filter(_.id === id) joinLeft
        (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
        (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
        (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
        (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
        (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations =>
      eventWithRelationsTupleToEventWithRelation(eventWithRelations)) map(_.headOption)
  }

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(event => event.endTime.nonEmpty && event.endTime > now ||
          event.endTime.isEmpty && event.startTime > twelveHoursAgo)
        .sortBy(event => (event.geographicPoint <-> geographicPoint, event.id))
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
      sortByDistanceToPoint(geographicPoint, eventWithRelationsTupleToEventWithRelation(eventWithRelations)))
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[EventWithRelations]] =
    geographicPointMethods.findGeographicPointOfCity(city) flatMap {
    case None => Future(Seq.empty)
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
        .sortBy(event => (event.geographicPoint <-> geographicPoint, event.id))
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
      sortByDistanceToPoint(geographicPoint, eventWithRelationsTupleToEventWithRelation(eventWithRelations)))
  }

  def findPassedInHourIntervalNear(hourInterval: Int, geographicPoint: Geometry, offset: Int,
                                   numberToReturn: Int): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()
    val xHoursAgo = now.minusHours(hourInterval)

    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(event => (event.startTime < now) && (event.startTime > xHoursAgo))
        .sortBy(event => (event.geographicPoint <-> geographicPoint, event.id))
        .drop(offset)
        .take(numberToReturn) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
  }

  def findAllByGenre(genreName: String, geographicPoint: Geometry, offset: Int, numberToReturn: Int)
  : Future[Seq[EventWithRelations]] = {
    val lowerCaseGenre = genreName.toLowerCase
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      genre <- genres if genre.name === lowerCaseGenre
      eventGenre <- eventsGenres if eventGenre.genreId === genre.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(event => event.endTime.nonEmpty && event.endTime > now ||
          event.endTime.isEmpty && event.startTime > twelveHoursAgo)
        .sortBy(event => (event.geographicPoint <-> geographicPoint, event.id))
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
      sortByDistanceToPoint(geographicPoint, eventWithRelationsTupleToEventWithRelation(eventWithRelations)))
  }

  def findAllNotFinishedByPlace(placeId: Long): Future[Seq[EventWithRelations]] = {
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
      eventWithRelationsTupleToEventWithRelation(eventWithRelations).sortBy(-_.event.startTime.getMillis))
  }


  def findAllPassedByPlace(placeId: Long): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()

    val query = for {
      place <- places if place.id === placeId
      eventPlace <- eventsPlaces if eventPlace.placeId === place.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(_.startTime < now)
        .sortBy(event => (event.startTime.desc, event.id)) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventPlace.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
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
        .sortBy(event => (event.startTime.desc, event.id)) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventOrganizer.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
  }

  def findAllPassedByOrganizer(organizerId: Long): Future[Seq[EventWithRelations]] = {
    val now = DateTime.now()

    val query = for {
      organizer <- organizers if organizer.id === organizerId
      eventOrganizer <- eventsOrganizers if eventOrganizer.organizerId === organizer.id
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
        optionalEventAddresses) <- events
        .filter(_.startTime < now)
        .sortBy(event => (event.startTime.desc, event.id)) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventOrganizer.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
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
        .sortBy(event => (event.startTime.desc, event.id)) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventArtist.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
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
        .sortBy(event => (event.startTime.desc, event.id)) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)

      if eventWithOptionalEventOrganizers._1.id === eventArtist.eventId
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
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
        .sortBy(event => (event.startTime.desc, event.id)) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
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
        .sortBy(event => (event.geographicPoint <-> geographicPoint, event.id)) joinLeft
          (eventsOrganizers join organizers on (_.organizerId === _.id)) on (_.id === _._1.eventId) joinLeft
          (eventsArtists join artists on (_.artistId === _.id)) on (_._1.id === _._1.eventId) joinLeft
          (eventsPlaces join places on (_.placeId === _.id)) on (_._1._1.id === _._1.eventId) joinLeft
          (eventsGenres join genres on (_.genreId === _.id)) on (_._1._1._1.id === _._1.eventId) joinLeft
          (eventsAddresses join addresses on (_.addressId === _.id)) on (_._1._1._1._1.id === _._1.eventId)
    } yield (eventWithOptionalEventOrganizers, optionalEventArtists, optionalEventPlaces, optionalEventGenres,
        optionalEventAddresses)

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
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

    db.run(query.result) map(eventWithRelations => eventWithRelationsTupleToEventWithRelation(eventWithRelations))
  }

  def save(eventWithRelations: EventWithRelations): Future[Event] = {
    val query = for {
      eventFound <- events.filter(_.facebookId === eventWithRelations.event.facebookId).result.headOption
      result <- eventFound.map(DBIO.successful).getOrElse(events returning events.map(_.id) += eventWithRelations.event)
    } yield result

    db.run(query) flatMap {
      case e: Event =>
        saveEventRelations(eventWithRelations)

      case id: Long =>
        saveEventRelations(eventWithRelations.copy(event = eventWithRelations.event.copy(id = Option(id))))
    }
  }

  def saveEventRelations(eventWithRelations: EventWithRelations): Future[Event] = {
    val eventId = eventWithRelations.event.id.getOrElse(0L)

    val eventuallyGenresResult =
      Future.sequence(eventWithRelations.genres map(genre => genreMethods.saveWithEventRelation(genre, eventId)))
    val eventuallyArtistsResult =
      Future.sequence(eventWithRelations.artists map(artist => artistMethods.saveWithEventRelation(artist, eventId)))
    val eventuallyOrganizersResult =
      Future.sequence(eventWithRelations.organizers map(organizer => organizerMethods.saveWithEventRelation(organizer, eventId)))
    val eventuallyPlacesResult =
      Future.sequence(eventWithRelations.places map(place => placeMethods.saveWithEventRelation(place, eventId)))
    val eventuallyAddressesResult =
      Future.sequence(eventWithRelations.addresses map(address => addressMethods.saveWithEventRelation(address, eventId)))

    val results = for {
      genresResult <- eventuallyGenresResult
      artistsResult <- eventuallyArtistsResult
      organizersResult <- eventuallyOrganizersResult
      placesResult <- eventuallyPlacesResult
      addressesResult <- eventuallyAddressesResult
    } yield (genresResult, artistsResult, organizersResult, placesResult, addressesResult)

    results map( _ => eventWithRelations.event)
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
                trackMethods.saveTracksEnumerator(tracksEnumerator)
                artist
              }
            }
            save(event) map Option.apply

          case None =>
            Future(None)
        }
    }
  }

  def getEventOnFacebookByFacebookId(eventFacebookId: String): Future[Option[EventWithRelations]] = WS
    .url("https://graph.facebook.com/" + facebookApiVersion + "/" + eventFacebookId)
    .withQueryString(
      "fields" -> "cover,description,name,start_time,end_time,owner,venue,place",
      "access_token" -> facebookToken)
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
      name = refactorEventOrPlaceName(name),
      description = formatDescription(maybeDescription),
      startTime = stringToDateTime(startTime),
      endTime = optionStringToOptionDateTime(endTime),
      imagePath = maybeCover,
      tariffRange = tariffMethods.findPricesInDescription(maybeDescription))

    val addresses = Try(Address(
      id = None,
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

        val eventuallyMaybeOrganizer =
          organizerMethods.getOrganizerInfo(eventWithRelationsAndMaybePlaceAndOwnerIds._2.maybeOwnerId)

        val eventuallyMaybePlace: Future[Option[PlaceWithAddress]] =
          eventWithRelationsAndMaybePlaceAndOwnerIds._2.maybePlaceId match {
            case Some(placeId) => placeMethods.getPlaceByFacebookId(placeId)
            case None => Future(None)
        }

        val eventuallyNormalizedWebsites: Future[Set[String]] = eventWithRelations.event.description match {
          case Some(description) => getNormalizedWebsitesInText(description)
          case None => Future(Set.empty)
        }

        for {
          maybeOrganizerWithAddress <- eventuallyMaybeOrganizer
          normalizedWebsites <- eventuallyNormalizedWebsites
          artistsFromDescription <- artistMethods.getFacebookArtistsByWebsites(normalizedWebsites)
          artistsFromTitle <- artistMethods.getEventuallyArtistsInEventTitle(
            eventWithRelationsAndMaybePlaceAndOwnerIds._1.event.name, normalizedWebsites)
          maybePlace <- eventuallyMaybePlace
          nonEmptyArtists = (artistsFromDescription.toVector ++ artistsFromTitle).distinct
          artistGenres = nonEmptyArtists.flatMap(_.genres map (_.genre))
          overGenres <- genreMethods.findOverGenres(artistGenres)
          genres = (artistGenres ++ overGenres).distinct
        } yield {

          val ticketSellers = tariffMethods.findTicketSellers(normalizedWebsites)

          val eventAddresses: Seq[Address] = eventWithRelations.addresses ++ Seq(maybePlace.flatMap(_.maybeAddress)).flatten

          Option(EventWithRelations(
            event = eventWithRelations.event.copy(ticketSellers = ticketSellers),
            addresses = eventAddresses,
            artists = nonEmptyArtists,
            organizers = Vector(maybeOrganizerWithAddress).flatten,
            places = Vector(maybePlace).flatten,
            genres = genres))
        }

      case Failure(t: Throwable) =>
        Logger.error("Event.facebookEventToEventWithRelations: from readFacebookEvent:\nMessage:\n" + t.getMessage +
          "\nFor facebook response:\n" + eventFacebookResponse.json)
        Future(None)
    }
  }

  def getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId: String): Future[Seq[String]] = WS
    .url("https://graph.facebook.com/" + facebookApiVersion + "/" + facebookId + "/events/")
    .withQueryString("access_token" -> facebookToken)
    .get()
    .map(readEventsIdsFromWSResponse)

  def readEventsIdsFromWSResponse(resp: WSResponse): Seq[String] = Try {
    val readFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readFacebookIds).flatten
  } match {
    case Success(facebookIds) => facebookIds
    case _ => Seq.empty
  }
}