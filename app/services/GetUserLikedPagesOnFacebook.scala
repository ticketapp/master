package services

import java.util.UUID
import javax.inject.Inject

import artistsDomain.{ArtistMethods, ArtistWithWeightedGenres, PatternAndArtist}
import com.mohiva.play.silhouette.api.LoginInfo
import database.{MyPostgresDriver, UserOrganizerRelation, UserPlaceRelation, UserArtistRelation}
import eventsDomain.EventMethods
import organizersDomain.OrganizerMethods
import placesDomain.PlaceMethods
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WS
import silhouette.OAuth2InfoDAO
import tracksDomain.TrackMethods

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Success, Try}


case class PageIdAndCategory(id: String, category: Option[String])


class GetUserLikedPagesOnFacebook @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                            protected val oAuth2InfoDAO: OAuth2InfoDAO,
                                            val utilities: Utilities,
                                            val artistMethods: ArtistMethods,
                                            val placeMethods: PlaceMethods,
                                            val organizerMethods: OrganizerMethods,
                                            val eventMethods: EventMethods,
                                            val trackMethods: TrackMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with FollowService {

  val facebookApiVersion = utilities.facebookApiVersion

  def getUserLikedPagesOnFacebook(loginInfo: LoginInfo, userUuid: UUID): Unit = findAccessToken(loginInfo) map {
    case Some(accessToken) =>
      getMusicPagesOnFacebook(accessToken, userUuid)
    case _ =>
  } recover {
    case NonFatal(e) =>
      play.api.Logger.error("GetUserLikedPagesOnFacebook.getUserLikedPagesOnFacebook: ", e)
  }

  def findAccessToken(loginInfo: LoginInfo): Future[Option[String]] = {
    oAuth2InfoDAO.find(loginInfo) map {
      case Some(oAuth2Info) =>
        Option(oAuth2Info.accessToken)
      case _ =>
        None
    } recover {
      case NonFatal(e) =>
        Logger.error("GetUserLikedPagesOnFacebook.getUserLikedPagesOnFacebook: ", e)
        None
    }
  }

  def getMusicPagesOnFacebook(facebookAccessToken: String, userUuid: UUID): Unit = WS
    .url("https://graph.facebook.com/" + facebookApiVersion + "/me")
    .withQueryString(
      "fields" -> "id,name,likes{id,name,category}",
      "access_token" -> facebookAccessToken)
    .get()
    .map(response => filterPages(response.json, userUuid, facebookAccessToken))

  def getNextFacebookPages(url: String, facebookAccessToken: String, userUuid: UUID): Unit = WS
    .url(url)
    .get()
    .map(response => filterPages(response.json, userUuid, facebookAccessToken))

  def filterPages(pages: JsValue, userUuid: UUID, facebookAccessToken: String): Unit = {
    val facebookTuples = facebookPageToPageIdAndCategory(pages)

    val artistPages = filterArtistPages(facebookTuples)

    val placePages = filterPlacePages(facebookTuples)

    val organizerPages = filterOrganizerPages(facebookTuples)
    
    val userArtists = makeRelationArtistUserOneByOne(artistPages, userUuid)

    val userPlaces = makeRelationPlaceUserOneByOne(placePages, userUuid)

    val userOrganizers = makeRelationOrganizerUserOneByOne(organizerPages, userUuid)

    for {
      artists <- userArtists
      places <- userPlaces
      organizers <- userOrganizers
    } yield {
      searchNextLikesPage(pages) match  {
        case Some(url) =>
          getNextFacebookPages(url, facebookAccessToken, userUuid)
        case _ =>
          Logger.info("GetUserLikedPagesOnFacebook.filterPages: Done")
      }
    }
  }

  def filterArtistPages(facebookTuples: Seq[PageIdAndCategory]): Seq[PageIdAndCategory] = {
    facebookTuples.filter { page =>
      page.category match {
        case Some(category) =>
          category.toLowerCase == "musician/band"
        case _ =>
          false
      }
    }
  }

  def filterPlacePages(facebookTuples: Seq[PageIdAndCategory]): Seq[PageIdAndCategory] = {
    facebookTuples.filter { page =>
      page.category match {
        case Some(category) =>
          category.toLowerCase == "concert venue" ||
            category.toLowerCase == "club" ||
            category.toLowerCase == "bar" ||
            category.toLowerCase == "arts/entertainment/nightlife"
        case _ =>
          false
      }
    }
  }

  def filterOrganizerPages(facebookTuples: Seq[PageIdAndCategory]): Seq[PageIdAndCategory] = {
    facebookTuples.filter { page =>
      page.category match {
        case Some(category) =>
          category.toLowerCase == "concert tour" ||
            category.toLowerCase == "non-profit organization"
        case _ =>
          false
      }
    }
  }

  def makeRelationArtistUserOneByOne(artists: Seq[PageIdAndCategory], userUuid: UUID): Future[Boolean] = artists.headOption
    match {
      case Some(nonEmptyPages) =>
        makeRelationArtistUser(nonEmptyPages, userUuid) map { artistRelation =>
          makeRelationArtistUserOneByOne(artists.tail, userUuid)
          artistRelation
        }
      case _ =>
        Logger.info("GetUserLikedPagesOnFacebook.makeArtistUserRelations: done")
        Future(true)
    }
  
  def makeRelationPlaceUserOneByOne(places: Seq[PageIdAndCategory], userUuid: UUID): Future[Boolean] = places.headOption
  match {
    case Some(nonEmptyPages) =>
      makeRelationPlaceUser(nonEmptyPages, userUuid) map { placeRelation =>
        makeRelationPlaceUserOneByOne(places.tail, userUuid)
        placeRelation
      }
    case _ =>
      Logger.info("GetUserLikedPagesOnFacebook.makePlacesUserRelations: done")
      Future(true)
  }
  
  def makeRelationOrganizerUserOneByOne(organizers: Seq[PageIdAndCategory], userUuid: UUID): Future[Boolean] = organizers.headOption
  match {
    case Some(nonEmptyPages) =>
      makeRelationOrganizerUser(nonEmptyPages, userUuid) map { organizerRelation =>
        makeRelationOrganizerUserOneByOne(organizers.tail, userUuid)
        organizerRelation
      }
    case _ =>
      Logger.info("GetUserLikedPagesOnFacebook.makeOrganizersUserRelations: done")
      Future(true)
  }

  def searchNextLikesPage(pages: JsValue): Option[String] = {
    val readNextFacebookPages: Reads[Option[String]] = (__ \ "next").readNullable[String]
    val jsonLikes: JsLookupResult = pages \ "likes"
    jsonLikes match {
      case JsDefined(likes) =>
        (pages \ "likes" \ "paging").asOpt[Option[String]](readNextFacebookPages).flatten
      case _ =>
        (pages \ "paging").asOpt[Option[String]](readNextFacebookPages).flatten
    }
  }

  def makeRelationArtistUser(facebookPageTuple: PageIdAndCategory, userUuid: UUID): Future[Boolean] = {
    val facebookId = facebookPageTuple.id
    artistMethods.findIdByFacebookId(facebookId) flatMap {
      case Some(artistId) =>
        followByArtistId(UserArtistRelation(userId = userUuid, artistId = artistId)) map {
          case followArtist if followArtist == 1 =>
            true
          case _ =>
            false
        }
      case _ =>
        artistMethods.getFacebookArtistByFacebookUrl(facebookId) flatMap {
          case Some(artistFound) =>
            artistMethods.save(artistFound) map { savedArtist =>
              savedArtist.id match {
                case Some(id) =>
                  followByArtistId(UserArtistRelation(userId = userUuid, artistId = id))

                  val patternAndArtist = PatternAndArtist(
                    searchPattern = savedArtist.name,
                    artistWithWeightedGenres = ArtistWithWeightedGenres(savedArtist, Vector.empty))
                  artistMethods.getArtistTracks(patternAndArtist)

                  val tracksEnumerator = artistMethods.getArtistTracks(patternAndArtist)
                  trackMethods.saveEnumeratorWithDelay(tracksEnumerator)
                  true
                case _ =>
                  false
              }
            }
          case _ =>
            Future(false)
        }
    }
  }

  def makeRelationPlaceUser(facebookPageTuple: PageIdAndCategory, userUuid: UUID): Future[Boolean] = {
    val facebookId = facebookPageTuple.id
    placeMethods.findIdByFacebookId(facebookId) flatMap {
      case Some(placeId) =>
        followByPlaceId(UserPlaceRelation(userId = userUuid, placeId = placeId)) map {
          case isFollowed if isFollowed == 1 =>

            true
          case _ =>
            false
        }
      case _ =>
        placeMethods.getPlaceByFacebookId(facebookId) flatMap {
          case Some(placeFound) =>
            placeMethods.saveWithAddress(placeFound) flatMap { savedPlace =>
              getPlaceOrOrganizerEvents(savedPlace.place.facebookId)
              savedPlace.place.id match {
                case Some(id) =>
                  followByPlaceId(UserPlaceRelation(userId = userUuid, placeId = id)) map {
                    case isFollowed if isFollowed == 1 =>
                      true
                    case _ =>
                      false
                  }
                case _ =>
                  Future(false)
              }
            }
          case _ =>
            Future(false)
        }
    }
  }

  def makeRelationOrganizerUser(facebookPageTuple: PageIdAndCategory, userUuid: UUID): Future[Boolean] = {
    val facebookId = facebookPageTuple.id
    organizerMethods.findIdByFacebookId(Option(facebookId)) flatMap {
      case Some(organizerId) =>
        followByOrganizerId(UserOrganizerRelation(userId = userUuid, organizerId = organizerId)) map {
          case isFollowed if isFollowed == 1 =>
            true
          case _ =>
            false
        }
      case _ =>
        organizerMethods.getOrganizerInfo(Option(facebookId)) flatMap {
          case Some(organizerFound) =>
            organizerMethods.saveWithAddress(organizerFound) flatMap { savedOrganizer =>
              getPlaceOrOrganizerEvents(savedOrganizer.organizer.facebookId)
              savedOrganizer.organizer.id match {
                case Some(id) =>
                  followByOrganizerId(UserOrganizerRelation(userId = userUuid, organizerId = id)) map {
                    case isFollowed if isFollowed == 1 =>
                      true
                    case _ =>
                      false
                  }
                case _ =>
                  Future(false)
              }
            }
          case _ =>
            Future(false)
        }
    }
  }

  def getPlaceOrOrganizerEvents(maybeFacebookId: Option[String]): Unit = {
    maybeFacebookId match {
      case Some(facebookId) =>
        eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map { eventIds =>
          saveEventsOneByOne(eventIds.toVector)
        }
      case _ =>
    }
  }

  def saveEventsOneByOne(eventIds: Seq[String]): Unit = eventIds.headOption match {
    case Some(eventId) =>
      eventMethods.saveFacebookEventByFacebookId(eventId) map { maybeEvent =>
        saveEventsOneByOne(eventIds.tail)
      }
    case _ =>
      Logger.info("GetUserLikedPagesOnFacebook.saveEvents: Done")
  }
  
  def readFacebookPage: Reads[PageIdAndCategory] = (
    (__ \ "id").read[String] and
    (__ \ "category").readNullable[String]
    )((maybeId: String, maybeCategory: Option[String]) =>
      PageIdAndCategory(maybeId, maybeCategory)
  )

  def facebookPageToPageIdAndCategory(pages: JsValue): Seq[PageIdAndCategory] = Try {
    val readFacebookPages: Reads[Seq[PageIdAndCategory]] = Reads.seq(readFacebookPage).map(_.toVector)
    val jsonLikes: JsLookupResult = pages \ "likes"
    jsonLikes match {
      case JsDefined(likes) =>
        (pages \ "likes" \ "data").asOpt[Seq[PageIdAndCategory]](readFacebookPages)
      case _ =>
        (pages \ "data").asOpt[Seq[PageIdAndCategory]](readFacebookPages)
    }
  } match {
    case Success(Some(facebookPagesTuple)) =>
      facebookPagesTuple
    case _ =>
      Seq.empty
  }
}
