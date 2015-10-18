package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models.{Artist, Genre, User, _}
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Enumeratee, Iteratee}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}


class ArtistController @Inject()(ws: WSClient,
                                 val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 val artistMethods: ArtistMethods,
                                 val trackMethods: TrackMethods,
                                 socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] {

  def getFacebookArtistsContaining(pattern: String) = Action.async {
    artistMethods.getEventuallyFacebookArtists(pattern).map { artists =>
      Ok(Json.toJson(artists))
    }
  }

  def artists = Action.async {
    artistMethods.findAll.map { artists =>
      Ok(Json.toJson(artists))
    }
  }

  def artistsSinceOffsetBy(number: Int, offset: Int) =  Action.async {
    artistMethods.findSinceOffset(number, offset).map { artists =>
      Ok(Json.toJson(artists))
    }
  } 

  def artist(id: Long) = Action.async {
    artistMethods.find(id).map { artist =>
      Ok(Json.toJson(artist))
    }
  }

  def artistByFacebookUrl(facebookUrl: String) = Action.async {
    artistMethods.findByFacebookUrl(facebookUrl).map { artist =>
      Ok(Json.toJson(artist))
    }
  }

  def artistsByGenre(genre: String, numberToReturn: Int, offset: Int) = Action.async {
    artistMethods.findAllByGenre(genre, numberToReturn, offset).map { artists =>
      Ok(Json.toJson(artists))
    }
  }

  def findArtistsContaining(pattern: String) = Action.async {
    artistMethods.findAllContaining(pattern) map { artists =>
      Ok(Json.toJson(artists)) }
  }

  val artistBindingForm = Form(
    mapping(
      "searchPattern" -> nonEmptyText(3),
      "artist" -> mapping(
        "facebookId" -> optional(nonEmptyText(2)),
        "artistName" -> nonEmptyText(2),
        "imagePath" -> optional(nonEmptyText(2)),
        "description" -> optional(nonEmptyText),
        "facebookUrl" -> nonEmptyText,
        "websites" -> seq(nonEmptyText(4)),
        /*"genres" -> seq(
          mapping(
            "name" -> nonEmptyText
          )(Genre.formApply)(Genre.formUnapply)),
        "tracks" -> seq(
          mapping(
            "trackId" -> nonEmptyText(8),
            "title" -> nonEmptyText,
            "url" -> nonEmptyText,
            "platform" -> nonEmptyText,
            "thumbnail" -> optional(nonEmptyText),
            "avatarUrl" -> optional(nonEmptyText),
            "artistName" -> nonEmptyText(2),
            "artistFacebookUrl" -> nonEmptyText(2),
            "redirectUrl" -> optional(nonEmptyText(2))
          )(Track.formApplyForTrackCreatedWithArtist)(Track.formUnapplyForTrackCreatedWithArtist)
        ),*/
        "likes" -> optional(number),
        "country" -> optional(nonEmptyText)
      )(artistMethods.formApply)(artistMethods.formUnapply)
    )(artistMethods.formWithPatternApply)(artistMethods.formWithPatternUnapply)
  )

  def createArtist = Action.async { implicit request =>
  artistBindingForm.bindFromRequest()
    .fold(
      formWithErrors => {
       Logger.error(formWithErrors.errorsAsJson.toString())
       Future(BadRequest(formWithErrors.errorsAsJson))
      },

      patternAndArtist => {
        artistMethods.save(patternAndArtist.artist) map { artist =>
         val artistId = artist.id
         val artistWithArtistId = patternAndArtist.artist.copy(id = artistId)
         val patternAndArtistWithArtistId = PatternAndArtist(patternAndArtist.searchPattern, artistWithArtistId)
         val tracksEnumerator = artistMethods.getArtistTracks(patternAndArtistWithArtistId)
         val toJsonTracks: Enumeratee[Set[Track], JsValue] = Enumeratee.map[Set[Track]]{ tracks =>
           val filteredTracks: Set[Track] = tracks.flatMap { track =>
             trackMethods.save(track)
             Some(track)
           }
           Json.toJson(filteredTracks)
         }
         val tracksJsonEnumerator = tracksEnumerator &> toJsonTracks

         Future { tracksEnumerator |>> Iteratee.foreach( a => a.map { trackMethods.save }) }
         Ok.chunked(tracksJsonEnumerator)
        }
      }
    )
  }

    def deleteArtist(artistId: Long) = Action.async {
      artistMethods.delete(artistId) map { result =>
        Ok(Json.toJson(result))
      }
    }
  
    def followArtistByArtistId(artistId : Long) = SecuredAction { implicit request =>
      val userId = request.identity.userID
//      artistMethods.followByArtistId(userId, artistId) match {
//        case Success(_) =>
//          Created
//        case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
//          Logger.error(s"ArtistController.followArtistByArtistId: user with id $userId already follows artist with id $artistId")
//          Conflict("This user already follows this artist.")
//        case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
//          Logger.error(s"ArtistController.followArtistByArtistId: there is no artist with the id $artistId")
//          NotFound
//        case Failure(unknownException) =>
//          Logger.error("ArtistController.followArtistByArtistId", unknownException)
//          Status(INTERNAL_SERVER_ERROR)
//      }
      Ok
    }
//
//    def unfollowArtistByArtistId(artistId : Long) = SecuredAction { implicit request =>
//      val userId = request.identity.UUID
//      artistMethods.unfollowByArtistId(userId, artistId) match {
//        case Success(1) =>
//          Ok
//        case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
//          Logger.error(s"The user (id: $userId) does not follow the artist (artistId: $artistId) or the artist does not exist.")
//          NotFound
//        case Failure(unknownException) =>
//          Logger.error("ArtistController.followArtistByArtistId", unknownException)
//          InternalServerError
//      }
//    }
//
//    def followArtistByFacebookId(facebookId : String) = SecuredAction { implicit request =>
//      val userId = request.identity.UUID
//      artistMethods.followByFacebookId(userId, facebookId) match {
//        case Success(_) =>
//          Created
//        case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
//          Logger.error(
//            s"""ArtistController.followArtistByFacebookId: user with id $userId already follows
//               |artist with facebook id $facebookId""".stripMargin)
//          Conflict("This user already follows this artist.")
//        case Failure(thereIsNoArtistForThisFacebookIdException: ThereIsNoArtistForThisFacebookIdException) =>
//          Logger.error(s"ArtistController.followArtistByFacebookId : there is no artist with the facebook id $facebookId")
//          NotFound("There is no artist with this id.")
//        case Failure(unknownException) =>
//          Logger.error("ArtistController.followArtistByFacebookId", unknownException)
//          Status(INTERNAL_SERVER_ERROR)
//      }
//    }
//
//    def getFollowedArtists = SecuredAction { implicit request =>
//      Ok(Json.toJson(artistMethods.getFollowedArtists(request.identity.UUID)))
//    }
//
//    def isArtistFollowed(artistId: Long) = SecuredAction { implicit request =>
//      Ok(Json.toJson(artistMethods.isFollowed(request.identity.UUID, artistId)))
//    }
}
