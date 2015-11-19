package services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import silhouette.OAuth2InfoDAO

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Success, Try}

class GetUserLikedPagesOnFacebook @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                            protected val oAuth2InfoDAO: OAuth2InfoDAO,
                                             val utilities: Utilities,
                                             val artistMethods: ArtistMethods,
                                             val placeMethods: PlaceMethods,
                                             val organizerMethods: OrganizerMethods,
                                             val eventMethods: EventMethods)
  extends HasDatabaseConfigProvider[MyPostgresDriver] with FollowService {

  val facebookApiVersion = utilities.facebookApiVersion

  def findUserLikedPagesOnFacebook(loginInfo: LoginInfo, userUuid: UUID): Unit = {
    getAccessToken(loginInfo) map {
      case Some(accessToken) =>
        findMusicPagesOnFacebook(accessToken, userUuid)
      case _ =>
    }
  }

  def getAccessToken(loginInfo: LoginInfo): Future[Option[String]] = {
    oAuth2InfoDAO.find(loginInfo) map {
      case Some(oAuth2Info) =>
        Option(oAuth2Info.accessToken)
      case _ =>
        None
    }
  }

  def findMusicPagesOnFacebook(facebookAccessToken: String, userUuid: UUID): Unit = {
    WS.url("https://graph.facebook.com/" + facebookApiVersion + "/me")
      .withQueryString(
        "fields" -> "id,name,events{place,owner,admins},likes{id, name, category, categories_list}",
        "access_token" -> facebookAccessToken)
    .get()
    .map { response =>
      filterPages(response, userUuid, facebookAccessToken)
    }
  }

  def nextFacebookPages(url: String, facebookAccessToken: String, userUuid: UUID): Unit = {
    WS.url(url)
      .get()
      .map { response =>
        filterPages(response, userUuid, facebookAccessToken)
      }
  }

  def filterPages(pages: WSResponse, userUuid: UUID, facebookAccessToken: String): Unit = {
    facebookPageToPageTuple(pages) foreach { facebookPageTuple =>
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
    findEventsIds(pages) map { eventId =>
      eventMethods.findEventOnFacebookByFacebookId(eventId) map {
        case Some(event) =>
          event.event.id match {
            case Some(id) =>
              follow(UserEventRelation(userUuid, id))
            case _ =>
          }
        case _ =>
      }
    }
    findNextPage(pages) match  {
      case Some(url) =>
        nextFacebookPages(url, facebookAccessToken, userUuid)
      case _ =>
    }
  }

  def findNextPage(pages: WSResponse): Option[String] = {
    val readNextFacebookPages: Reads[Option[String]] = (__ \ "next").readNullable[String]
    val jsonLikes: JsLookupResult = pages.json \ "likes"
    jsonLikes match {
      case JsDefined(likes) =>
        (pages.json \ "likes" \ "paging").asOpt[Option[String]](readNextFacebookPages).flatten
      case _ =>
        (pages.json \ "paging").asOpt[Option[String]](readNextFacebookPages).flatten
    }
  }

  def makeRelationArtistUser(facebookPageTuple: (String, Option[String]), userUuid: UUID): Unit = {
    artistMethods.findIdByFacebookId(facebookPageTuple._1) map {
      case Some(artistId) =>
        followByArtistId(UserArtistRelation(userId = userUuid, artistId = artistId)) map {
          case followArtist if followArtist == 1 =>
            true
          case _ =>
            false
        }
      case _ =>
        artistMethods.getFacebookArtistByFacebookUrl(facebookPageTuple._1) map {
          case Some(artistFound) =>
            artistMethods.save(artistFound) map { savedArtist =>
              savedArtist.id match {
                case Some(id) =>
                  followByArtistId(UserArtistRelation(userId = userUuid, artistId = id))
                case _ =>
              }
            }
          case _ =>
        }
    }
  }

  def makeRelationPlaceUser(facebookPageTuple: (String, Option[String]), userUuid: UUID): Unit = {
    placeMethods.findIdByFacebookId(facebookPageTuple._1) map {
      case Some(placeId) =>
        followByPlaceId(UserPlaceRelation(userId = userUuid, placeId = placeId))
      case _ =>
        placeMethods.getPlaceByFacebookId(facebookPageTuple._1) map {
          case Some(placeFound) =>
            placeMethods.saveWithAddress(placeFound) map { savedPlace =>
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
    organizerMethods.findIdByFacebookId(Option(facebookPageTuple._1)) map {
      case Some(organizerId) =>
        followByOrganizerId(UserOrganizerRelation(userId = userUuid, organizerId = organizerId))
      case _ =>
        organizerMethods.getOrganizerInfo(Option(facebookPageTuple._1)) map {
          case Some(organizerFound) =>
            organizerMethods.saveWithAddress(organizerFound) map { savedOrganizer =>
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

  def facebookPageToPageTuple(pages: WSResponse): Seq[(String, Option[String])] = Try {
    val readFacebookPages: Reads[Seq[(String, Option[String])]] = Reads.seq(readFacebookPage).map(_.toVector)
    val jsonLikes: JsLookupResult = pages.json \ "likes"
    jsonLikes match {
      case JsDefined(likes) =>
        (pages.json \ "likes" \ "data").asOpt[Seq[(String, Option[String])]](readFacebookPages)
      case _ =>
        (pages.json \ "data").asOpt[Seq[(String, Option[String])]](readFacebookPages)
    }
  } match {
    case Success(Some(facebookPagesTuple)) =>
      facebookPagesTuple 
    case _ => Seq.empty
  }

  def findEventsIds(pages: WSResponse): Seq[String] = Try {
    val readFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String]).map(_.toVector)
    (pages.json \ "events").asOpt[Seq[Option[String]]](readFacebookIds)
  } match {
    case Success(Some(facebookIds)) => facebookIds collect {
      case Some(id) => id
    }
    case _ => Seq.empty
  }
}
