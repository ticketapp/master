package models

import javax.inject.Inject

import com.vividsolutions.jts.geom.Geometry
import json.JsonHelper._
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{FollowService, MyPostgresDriver, Utilities}
import silhouette.DBTableDefinitions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps


case class Event(id: Option[Long],
                 facebookId: Option[String],
                 isPublic: Boolean,
                 isActive: Boolean,
                 name: String,
                 geographicPoint: Option[Geometry],
                 description: Option[String],
                 startTime: DateTime,
                 endTime: Option[DateTime],
                 ageRestriction: Int,
                 tariffRange: Option[String],
                 ticketSellers: Option[String],
                 imagePath: Option[String])

case class EventWithRelations(event: Event,
                              organizers: Seq[Organizer] = Vector.empty,
                              artists: Seq[Artist] = Vector.empty,
                              places: Seq[Place] = Vector.empty,
                              genres: Seq[Genre] = Vector.empty,
                              addresses: Seq[Address] = Vector.empty)


class EventMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val artistMethods: ArtistMethods,
                             val tariffMethods: TariffMethods,
                             val genreMethods: GenreMethods,
                             val geographicPointMethods: SearchGeographicPoint,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with FollowService
    with DBTableDefinitions
    with MyDBTableDefinitions {

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

  def eventWithRelationsTupleToEventWithRelationClass(eventWithRelations: Seq[((Event, Option[(EventOrganizerRelation, Organizer)]), Option[(EventArtistRelation, Artist)], Option[(EventPlaceRelation, Place)], Option[(EventGenreRelation, Genre)], Option[(EventAddressRelation, Address)])])
  : Vector[EventWithRelations] = {
    val groupedByEvents = eventWithRelations.groupBy(_._1._1)

    groupedByEvents.map { eventWithOptionalRelations =>
      val event = eventWithOptionalRelations._1
      val relations = eventWithOptionalRelations._2
      val organizers = relations collect {
        case ((_, Some((_, organizer: Organizer))), _, _, _, _) => organizer
      }
      val artists = relations collect {
        case ((_, _), Some((_, artist: Artist)), _, _, _) => artist
      }
      val places = relations collect {
        case ((_, _), _, Some((_, place: Place)), _, _) => place
      }
      val genres = relations collect {
        case ((_, _), _, _, Some((_, genre: Genre)), _) => genre
      }
      val addresses = relations collect {
        case ((_, _), _, _, _, Some((_, address: Address))) => address
      }

      EventWithRelations(event, organizers, artists, places, genres, addresses)
    }.toVector
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
    val xHoursAgo = now.minusHours(hourInterval)

    val query = for {
      (((((eventWithOptionalEventOrganizers), optionalEventArtists), optionalEventPlaces), optionalEventGenres),
      optionalEventAddresses) <- events
        .filter(event =>
          (event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > xHoursAgo))
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
          || (event.endTime.isEmpty && event.startTime > twelveHoursAgo)))
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
        .take(20)
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
        .sortBy(_.geographicPoint <-> geographicPoint)
        .take(20) joinLeft
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
//        """SELECT e.* FROM eventsAddresses eA
//          | INNER JOIN events e ON e.eventId = eA.eventId AND
//          |  (e.endTime IS NOT NULL AND e.endTime > CURRENT_TIMESTAMP
//          |    OR e.endTime IS NULL AND e.startTime > CURRENT_TIMESTAMP - interval '12 hour')
//          | INNER JOIN addresses a ON a.addressId = eA.addressId
//          |   WHERE a.city LIKE '%'||{patternLowCase}||'%'
//          |LIMIT 50""".stripMargin)
//        .on('patternLowCase -> cityPattern.toLowerCase)
//
//    val query =
    //    .map(getPropertiesOfEvent)
    Future { Seq.empty }
  }

  def save(eventWithRelations: EventWithRelations): Future[Event] =
    db.run((for {
      eventFound <- events.filter(_.facebookId === eventWithRelations.event.facebookId).result.headOption
      result <- eventFound.map(DBIO.successful).getOrElse(events returning events.map(_.id) += eventWithRelations.event)
    } yield result match {
        case e: Event =>
          e
//          organizerMethods.saveEventRelations(EventOrganizerRelation(e.id.get, eventWithRelations.organizers))
        case id: Long => eventWithRelations.event.copy(id = Option(id))
      }).transactionally)
  /*
  def save(event: Event): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT insertEvent({facebookId}, {isPublic}, {isActive}, {name}, {geographicPoint}, {description},
           |{startTime}, {endTime}, {imagePath}, {ageRestriction}, {tariffRange}, {ticketSellers})""".stripMargin)
        .on(
          'facebookId -> event.facebookId,
          'isPublic -> event.isPublic,
          'isActive -> event.isActive,
          'name -> event.name,
          'geographicPoint -> event.geographicPoint,
          'description -> event.description,
          'startTime -> event.startTime,
          'endTime -> event.endTime,
          'imagePath -> event.imagePath,
          'ageRestriction -> event.ageRestriction,
          'tariffRange -> event.tariffRange,
          'ticketSellers -> event.ticketSellers)
        .as(scalar[Option[Long]].single) match {
        case None =>
          Logger.error("Event.save: event could not be saved")
          None
        case Some(eventId: Long) =>
//          event.organizers.foreach { organizer => organizerMethods.saveWithEventRelation(organizer, eventId) }
          event.tariffs.foreach { tariff => Tariff.save(tariff.copy(eventId = eventId)) }
          event.artists.foreach { artist => Artist.saveWithEventRelation(artist, eventId) }
          event.genres.foreach { genre => Genre.saveWithEventRelation(genre, eventId) }
          event.addresses.foreach { address => Address.saveAddressAndEventRelation(address, eventId) }
          Option(eventId)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Event.save: " + e.getMessage)
  }
*/
  def delete(id: Long): Future[Int] = db.run(events.filter(_.id === id).delete)

  def update(event: Event): Future[Int] = db.run(events.filter(_.id === event.id).update(event))

  def saveFacebookEventByFacebookId(eventFacebookId: String): Future[Event] = {
    val maybeEvent = db.run(events.filter(_.facebookId === eventFacebookId).result.headOption)
    maybeEvent flatMap {
      case Some(event) =>
        Future(event)
      case None =>
        findEventOnFacebookByFacebookId(eventFacebookId) flatMap { event =>
          save(event)
        }
    }
  }

  def findEventOnFacebookByFacebookId(eventFacebookId: String): Future[EventWithRelations] = {
    WS.url("https://graph.facebook.com/" + utilities.facebookApiVersion + "/" + eventFacebookId)
      .withQueryString(
        "fields" -> "cover,description,name,start_time,end_time,owner,venue,place",
        "access_token" -> utilities.facebookToken)
      .get()
      .flatMap { readFacebookEvent }
  }

  def readFacebookEvent(eventFacebookResponse: WSResponse): Future[EventWithRelations] = {
    val eventRead = (
      (__ \ "description").readNullable[String] and
        (__ \ "cover" \ "source").read[String] and
        (__ \ "name").read[String] and
        (__ \ "id").readNullable[String] and
        (__ \ "start_time").readNullable[String] and
        (__ \ "endTime").readNullable[String] and
        (__ \ "venue" \ "street").readNullable[String] and
        (__ \ "venue" \ "zip").readNullable[String] and
        (__ \ "venue" \ "city").readNullable[String] and
        (__ \ "owner" \ "id").readNullable[String] and
        (__ \ "place" \ "id").readNullable[String]
      )((description: Option[String], source: String, name: String, facebookId: Option[String],
         startTime: Option[String], endTime: Option[String], street: Option[String], zip: Option[String],
         city: Option[String], maybeOwnerId: Option[String], maybePlaceId: Option[String]) => {

      val eventuallyOrganizer = organizerMethods.getOrganizerInfo(maybeOwnerId)
      val address = new Address(None, None, city, zip, street)

      val normalizedWebsites: Set[String] = utilities.getNormalizedWebsitesInText(description)
      val ticketSellers = tariffMethods.findTicketSellers(normalizedWebsites)
      val eventuallyMaybeArtistsFromDescription = artistMethods.getFacebookArtistsByWebsites(normalizedWebsites)
      val eventuallyMaybeArtistsFromTitle =
        artistMethods.getEventuallyArtistsInEventTitle(name, normalizedWebsites)
      // !!!!!!!!!!!!!!!!!!!!!!!!!!!
      val eventuallyTryPlace = None//placeMethods.getPlaceByFacebookId(maybePlaceId)

      for {
        organizer <- eventuallyOrganizer
        artistsFromDescription <- eventuallyMaybeArtistsFromDescription
        artistsFromTitle <- eventuallyMaybeArtistsFromTitle
//        optionPlace <- eventuallyTryPlace
      } yield {

        val nonEmptyArtists = (artistsFromDescription.flatten.toList ++ artistsFromTitle).distinct
        artistMethods.saveArtistsAndTheirTracks(nonEmptyArtists)

        val normalizedStartTime: DateTime = startTime match {
          case Some(matchedDate) =>
            utilities.stringToDateTime(matchedDate)
          case None =>
          new DateTime()
        }

        val normalizedEndTime = endTime match {
          case Some(matchedDate) =>
            Option(utilities.stringToDateTime(matchedDate))
          case None => None
        }


//        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//        val eventGenres = (nonEmptyArtists.flatMap(_.genres) ++
//          nonEmptyArtists.flatMap(artist => genreMethods.findOverGenres(artist.genres))).distinct

        val event = EventWithRelations(
          event = Event(None, facebookId, isPublic = true, isActive = true, utilities.refactorEventOrPlaceName(name), None,
            utilities.formatDescription(description), normalizedStartTime/*,
            formatDate(endTime)*/, normalizedEndTime, 16, tariffMethods.findPrices(description), ticketSellers, Option(source)),
          genres = Vector.empty)

        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//        savePlaceEventRelationIfPossible(optionPlace, event)
        event
      }
    })
    try {
      eventFacebookResponse.json.as[Future[EventWithRelations]](eventRead)
    } catch {
      case e: Exception => throw new Exception("Empty event read by Event.readFacebookEvent" + e.getMessage)
    }
  }

//  def savePlaceEventRelationIfPossible(optionPlace: Option[Place], event: Event): Unit = {
//    optionPlace match {
//      case Some(place) =>
//        place.id match {
//          case Some(id) => saveEventWithGeographicPointAndPlaceRelation(event, id, place.geographicPoint)
//          case None => Logger.error("Event.readFacebookEvent: place without id found")
//        }
//      case None =>
//        Logger.error("Event.readFacebookEvent: the place is in error")
//    }
//  }

  def getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId: String): Future[Seq[String]] = {
    WS.url("https://graph.facebook.com/" + utilities.facebookApiVersion + "/" + facebookId + "/events/")
      .withQueryString("access_token" -> utilities.facebookToken)
      .get()
      .map { readEventsIdsFromWSResponse }
  }

  def readEventsIdsFromWSResponse(resp: WSResponse): Seq[String] = {
    val readSoundFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readSoundFacebookIds).flatten
  }
}