package services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WS
import silhouette.OAuth2InfoDAO

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Success, Try}

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

  def getMusicPagesOnFacebook(facebookAccessToken: String, userUuid: UUID): Unit =
    WS.url("https://graph.facebook.com/" + facebookApiVersion + "/me")
      .withQueryString(
        "fields" -> "id,name,likes{id, name, category, categories_list}",
        "access_token" -> facebookAccessToken)
    .get()
    .map { response =>
      filterPages(response.json, userUuid, facebookAccessToken)
    }

  def getNextFacebookPages(url: String, facebookAccessToken: String, userUuid: UUID): Unit = WS
    .url(url)
    .get()
    .map(response => filterPages(response.json, userUuid, facebookAccessToken))


  def filterPages(pages: JsValue, userUuid: UUID, facebookAccessToken: String): Unit = {
    facebookPageToPageTuple(pages) foreach { facebookPageTuple =>
      Thread.sleep(200)
      facebookPageTuple._2 match {
        case Some(artist) if artist.toLowerCase == "musician/band" =>
          makeRelationArtistUser(facebookPageTuple, userUuid)
        case Some(place) if place.toLowerCase == "concert venue" ||
          place.toLowerCase == "club" ||
          place.toLowerCase == "bar" ||
          place.toLowerCase == "arts/entertainment/nightlife" =>
          makeRelationPlaceUser(facebookPageTuple, userUuid)
        case Some(organizer) if organizer.toLowerCase == "concert tour" || organizer.toLowerCase == "non-profit organization" =>
          makeRelationOrganizerUser(facebookPageTuple, userUuid)
        case _ =>
      }
    }

    searchNextLikesPage(pages) match  {
      case Some(url) =>
        getNextFacebookPages(url, facebookAccessToken, userUuid)
      case _ =>
        Logger.info("getUserLikedPagesOnFacebook: Done")
    }
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

  def makeRelationArtistUser(facebookPageTuple: (String, Option[String]), userUuid: UUID): Unit = {
    val facebookId = facebookPageTuple._1
    artistMethods.findIdByFacebookId(facebookId) map {
      case Some(artistId) =>
        followByArtistId(UserArtistRelation(userId = userUuid, artistId = artistId)) map {
          case followArtist if followArtist == 1 =>
          case _ =>
        }
      case _ =>
        artistMethods.getFacebookArtistByFacebookUrl(facebookId) map {
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
                case _ =>
              }
            }
          case _ =>
        }
    }
  }

  def getPlaceOrOrganizerEvents(maybeFacebookId: Option[String]): Unit = {
    maybeFacebookId match {
      case Some(facebookId) =>
        eventMethods.getEventsFacebookIdByPlaceOrOrganizerFacebookId(facebookId) map {
          _.map { eventId =>
            Thread.sleep(200)
            eventMethods.saveFacebookEventByFacebookId(eventId)
          }
        }
      case _ =>
    }
  }

  def makeRelationPlaceUser(facebookPageTuple: (String, Option[String]), userUuid: UUID): Unit = {
    val facebookId = facebookPageTuple._1
    placeMethods.findIdByFacebookId(facebookId) map {
      case Some(placeId) =>
        followByPlaceId(UserPlaceRelation(userId = userUuid, placeId = placeId))
      case _ =>
        placeMethods.getPlaceByFacebookId(facebookId) map {
          case Some(placeFound) =>
            placeMethods.saveWithAddress(placeFound) map { savedPlace =>
              getPlaceOrOrganizerEvents(savedPlace.place.facebookId)
              savedPlace.place.id match {
                case Some(id) =>
                  followByPlaceId(UserPlaceRelation(userId = userUuid, placeId = id))
                case _ =>
              }
            }
          case _ =>
        }
    }
  }

  def makeRelationOrganizerUser(facebookPageTuple: (String, Option[String]), userUuid: UUID): Unit = {
    val facebookId = facebookPageTuple._1
    organizerMethods.findIdByFacebookId(Option(facebookId)) map {
      case Some(organizerId) =>
        followByOrganizerId(UserOrganizerRelation(userId = userUuid, organizerId = organizerId))
      case _ =>
        organizerMethods.getOrganizerInfo(Option(facebookId)) map {
          case Some(organizerFound) =>
            organizerMethods.saveWithAddress(organizerFound) map { savedOrganizer =>
              getPlaceOrOrganizerEvents(savedOrganizer.organizer.facebookId)
              savedOrganizer.organizer.id match {
                case Some(id) =>
                  followByOrganizerId(UserOrganizerRelation(userId = userUuid, organizerId = id))
                case _ =>
              }
            }
          case _ =>
        }
    }
  }
  
  def readFacebookPage: Reads[(String, Option[String])] = (
    (__ \ "id").read[String] and
    (__ \ "category").readNullable[String]
    )((maybeId: String, maybeCategory: Option[String]) =>
      (maybeId, maybeCategory)
  )

  def facebookPageToPageTuple(pages: JsValue): Seq[(String, Option[String])] = Try {
    val readFacebookPages: Reads[Seq[(String, Option[String])]] = Reads.seq(readFacebookPage).map(_.toVector)
    val jsonLikes: JsLookupResult = pages \ "likes"
    jsonLikes match {
      case JsDefined(likes) =>
        (pages \ "likes" \ "data").asOpt[Seq[(String, Option[String])]](readFacebookPages)
      case _ =>
        (pages \ "data").asOpt[Seq[(String, Option[String])]](readFacebookPages)
    }
  } match {
    case Success(Some(facebookPagesTuple)) =>
      facebookPagesTuple 
    case _ =>
      Seq.empty
  }
}
