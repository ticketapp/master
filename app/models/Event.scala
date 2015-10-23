package models

import java.util.UUID
import javax.inject.Inject

import com.vividsolutions.jts.geom.{Geometry, GeometryFactory, Point}
import json.JsonHelper._
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}
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
                 imagePath: Option[String])/*,
                 organizers: List[OrganizerWithAddress],
                 artists: List[Artist],
                 tariffs: List[Tariff],
                 addresses: List[Address],
                 places: List[Place] = List.empty,
                 genres: Seq[Genre] = Seq.empty)*/


class EventMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val placeMethods: PlaceMethods,
                             val artistMethods: ArtistMethods,
                             val tariffMethods: TariffMethods,
                             val geographicPointMethods: GeographicPointMethods,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with DBTableDefinitions with MyDBTableDefinitions {

  val geometryFactory = new GeometryFactory()

  def formApply(name: String, geographicPoint: Option[String], description: Option[String], startTime: DateTime,
                endTime: Option[DateTime], ageRestriction: Int, tariffRange: Option[String], ticketSellers: Option[String],
                imagePath: Option[String]/*, tariffs: List[Tariff], addresses: List[Address]*/): Event =
    new Event(None, None, true, true, name, geographicPointMethods.optionStringToOptionPoint(geographicPoint), description, startTime, endTime, ageRestriction,
      tariffRange, ticketSellers, imagePath)//, List.empty, List.empty, tariffs, addresses)

  def formUnapply(event: Event) = {
    Some((event.name, Option(event.geographicPoint.getOrElse("").toString), event.description, event.startTime, event.endTime, event.ageRestriction,
      event.tariffRange, event.ticketSellers, event.imagePath/*, event.tariffs, event.addresses*/))
  }

//  def getPropertiesOfEvent(event: Event): Event = event.eventId match {
//    case None => throw new DAOException("Event.getPropertiesOfEvent: event without id has been found")
//    case Some(eventId) => event.copy(
////      organizers = organizerMethods.findAllByEvent(event),
//      artists = Artist.findAllByEvent(event),
//      tariffs = Tariff.findAllByEvent(event),
//      places = placeMethods.findAllByEvent(eventId),
//      genres = Genre.findAllByEvent(eventId),
//      addresses = Address.findAllByEvent(event))
//  }

  def find(id: Long): Future[Option[Event]] = {
    val query = events.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val query = events
      .filter(event =>
        (event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
      .sortBy(_.geographicPoint <-> geographicPoint)
      .drop(numberToReturn)
      .take(offset)
    db.run(query.result)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Event]] = geographicPointMethods
    .findGeographicPointOfCity(city) flatMap {
    case None => Future { Seq.empty }
    case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
  }

  def findInPeriodNear(hourInterval: Int, geographicPoint: Geometry, offset: Int, numberToReturn: Int): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = events
      .filter(event =>
        (event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
      .sortBy(_.geographicPoint <-> geographicPoint)
      .drop(numberToReturn)
      .take(offset)
    db.run(query.result)
  }

    def findPassedInHourIntervalNear(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): Future[Seq[Event]] = {
      val now = DateTime.now()
      val xHoursAgo = now.minusHours(hourInterval)

      val query = events
        .filter(event => (event.startTime < now) || (event.startTime > xHoursAgo))
        .sortBy(_.geographicPoint <-> geographicPoint)
        .drop(numberToReturn)
        .take(offset)
      db.run(query.result)
    }

  def findAllByGenre(genreName: String, geographicPoint: Geometry, offset: Int, numberToReturn: Int): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      genre <- genres if genre.name === genreName
      eventGenre <- eventsGenres if eventGenre.genreId === genre.id
      event <- events if event.id === eventGenre.eventId &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.geographicPoint <-> geographicPoint)
      .drop(offset)
      .take(numberToReturn)
      .result)
  }

  def findAllByPlace(placeId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      place <- places if place.id === placeId
      eventPlace <- eventsPlaces if eventPlace.placeId === place.id
      event <- events if event.id === eventPlace.eventId &&
        ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }


  def findAllPassedByPlace(placeId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()

    val query = for {
      place <- places if place.id === placeId
      eventPlace <- eventsPlaces if eventPlace.placeId === place.id
      event <- events if event.id === eventPlace.eventId && event.startTime < now
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }
  
    def findAllByOrganizer(organizerId: Long): Future[Seq[Event]] = {
      val now = DateTime.now()
      val twelveHoursAgo = now.minusHours(12)

      val query = for {
        organizer <- organizers if organizer.id === organizerId
        eventOrganizer <- eventsOrganizers if eventOrganizer.organizerId === organizer.id
        event <- events if event.id === eventOrganizer.eventId &&
        ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
      } yield event

      //getEventProperties
      db.run(query.sortBy(_.startTime.desc).result)
    }

  def findAllPassedByOrganizer(organizerId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()

    val query = for {
      organizer <- organizers if organizer.id === organizerId
      eventOrganizer <- eventsOrganizers
      event <- events if event.id === eventOrganizer.eventId && event.startTime < now
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }

  def findAllByArtist(facebookUrl: String): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      artist <- artists if artist.facebookUrl === facebookUrl
      eventArtist <- eventsArtists if eventArtist.artistId === artist.id
      event <- events if event.id === eventArtist.eventId &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }

  def findAllPassedByArtist(artistId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()

    val query = for {
      artist <- artists if artist.id === artistId
      eventArtist <- eventsArtists
      event <- events if event.id === eventArtist.eventId && event.startTime < now
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }

  def findAllContaining(pattern: String): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      event <- events if (event.name.toLowerCase like s"%$lowercasePattern%") &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    db.run(query.take(20).result)
  }

  def findAllContaining(pattern: String, geographicPoint: Geometry): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      event <- events if (event.name.toLowerCase like s"%$lowercasePattern%") &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    db.run(query.sortBy(_.geographicPoint <-> geographicPoint).take(20).result)
  }

  def findAllByCityPattern(cityPattern: String): Future[Seq[Event]] = {
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

  def save(event: Event): Future[Event] = {
    val query = events returning events.map(_.id) into ((event, id) => event.copy(id = Some(id))) += event
    db.run(query)
  }
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

  def follow(userEventRelation: UserEventRelation): Future[Int] = db.run(eventsFollowed += userEventRelation)

  def unfollow(userEventRelation: UserEventRelation): Future[Int] = db.run(
   eventsFollowed
     .filter(eventFollowed =>
        eventFollowed.userId === userEventRelation.userId && eventFollowed.eventId === userEventRelation.eventId)
     .delete)

  def getFollowed(userId: UUID): Future[Seq[Event] ]= {
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

  def saveFacebookEventByFacebookId(eventFacebookId: String): Future[Event] =
    findEventOnFacebookByFacebookId(eventFacebookId) flatMap { save }

  def findEventOnFacebookByFacebookId(eventFacebookId: String): Future[Event] = {
    WS.url("https://graph.facebook.com/" + utilities.facebookApiVersion + "/" + eventFacebookId)
      .withQueryString(
        "fields" -> "cover,description,name,start_time,end_time,owner,venue,place",
        "access_token" -> utilities.facebookToken)
      .get()
      .flatMap { readFacebookEvent }
  }

  def readFacebookEvent(eventFacebookResponse: WSResponse): Future[Event] = {
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
        artistMethods.getEventuallyArtistsInEventTitle(artistMethods.splitArtistNamesInTitle(name), normalizedWebsites)
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

        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//        val eventGenres = (nonEmptyArtists.flatMap(_.genres) ++
//                          nonEmptyArtists.flatMap(artist => Genre.findOverGenres(artist.genres))).distinct

        val event = Event(None, facebookId, isPublic = true, isActive = true, utilities.refactorEventOrPlaceName(name), None,
        utilities.formatDescription(description), new DateTime()/*formatDate(startTime).getOrElse(new Date()),
        formatDate(endTime)*/, Option(new DateTime()), 16, tariffMethods.findPrices(description), ticketSellers, Option(source)/*, List(organizer).flatten,
        nonEmptyArtists, List.empty, List(address), List.empty, eventGenres*/)

        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//        savePlaceEventRelationIfPossible(optionPlace, event)
        event
      }
    })
    try {
      eventFacebookResponse.json.as[Future[Event]](eventRead)
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