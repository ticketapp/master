package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models._
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
import services.Utilities

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Failure


class ArtistController @Inject()(val messagesApi: MessagesApi,
                                 val utilities: Utilities,
                                 val env: Environment[User, CookieAuthenticator],
                                 val artistMethods: ArtistMethods,
                                 val trackMethods: TrackMethods,
                                 socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] with artistFormsTrait {

  def getFacebookArtistsContaining(pattern: String) = Action.async {
    artistMethods.getEventuallyFacebookArtists(pattern).map { artists =>
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

  def createArtist = Action.async { implicit request =>
  artistBindingForm.bindFromRequest()
    .fold(
      formWithErrors => {
       Logger.error(formWithErrors.errorsAsJson.toString())
       Future(BadRequest(formWithErrors.errorsAsJson))
      },

      patternAndArtist => {
        artistMethods.save(ArtistWithWeightedGenres(patternAndArtist.artistWithWeightedGenre.artist, Vector.empty)) map { artist =>
         val artistId = artist.id
         val artistWithArtistId = patternAndArtist.artistWithWeightedGenre.artist.copy(id = artistId)
         val patternAndArtistWithArtistId =
           PatternAndArtist(patternAndArtist.searchPattern, ArtistWithWeightedGenres(artistWithArtistId, Vector.empty))
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
  
  def followArtistByArtistId(artistId : Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    Logger.info(userId.toString)
    artistMethods.followByArtistId(UserArtistRelation(userId, artistId)) map {
      case 1 =>
        Created
      case _ =>
        Logger.error("ArtistController.followArtistByArtistId:  artistMethods.followByArtistId did not return 1!")
        InternalServerError("ArtistController.followArtistByArtistId:  artistMethods.followByArtistId did not return 1!")
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.UNIQUE_VIOLATION =>
        Logger.error(s"ArtistController.followArtistByArtistId: user with id $userId already follows artist with id $artistId")
        Conflict("This user already follows this artist.")
      case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
        Logger.error(s"ArtistController.followArtistByArtistId: there is no artist with the id $artistId")
        NotFound
      case unknownException =>
        Logger.error("ArtistController.followArtistByArtistId", unknownException)
        InternalServerError
    }
  }

  def unfollowArtistByArtistId(artistId : Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    artistMethods.unfollowByArtistId(UserArtistRelation(userId, artistId)) map {
      case 1 =>
        Ok
      case _ =>
        Logger.error("ArtistController.unfollowArtistByArtistId:  artistMethods.unfollowByArtistId did not return 1!")
        InternalServerError("ArtistController.unfollowArtistByArtistId:  artistMethods.unfollowByArtistId did not return 1!")
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the artist (artistId: $artistId) or the artist does not exist.")
        NotFound
      case unknownException =>
        Logger.error("ArtistController.unfollowArtistByArtistId", unknownException)
        InternalServerError
    }
  }

  def followArtistByFacebookId(facebookId : String) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    artistMethods.followByFacebookId(userId, facebookId) map {
      case 1 =>
        Created
      case _ =>
        Logger.error("ArtistController.followArtistByFacebookId:  artistMethods.followByArtistId did not return 1!")
        InternalServerError("ArtistController.followArtistByFacebookId:  artistMethods.followByArtistId did not return 1!")
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.UNIQUE_VIOLATION =>
        Logger.error(
          s"""ArtistController.followArtistByFacebookId: user with id $userId already follows
             |artist with facebook id $facebookId""".stripMargin)
        Conflict("This user already follows this artist.")
      case thereIsNoArtistForThisFacebookIdException: ThereIsNoArtistForThisFacebookIdException =>
        Logger.error(s"ArtistController.followArtistByFacebookId : there is no artist with the facebook id $facebookId")
        NotFound("There is no artist with this id.")
      case unknownException =>
        Logger.error("ArtistController.followArtistByFacebookId", unknownException)
        InternalServerError
    }
  }

  def getFollowedArtists = SecuredAction.async { implicit request =>
    artistMethods.getFollowedArtists(request.identity.uuid) map { artists =>
      Ok(Json.toJson(artists))
    } recover {
      case e =>
        Logger.error("ArtistController.getFollowedArtists: ", e)
        InternalServerError
    }
  }

  def isArtistFollowed(artistId: Long) = SecuredAction.async { implicit request =>
    artistMethods.isFollowed(UserArtistRelation(request.identity.uuid, artistId)) map { boolean =>
      Ok(Json.toJson(boolean))
    } recover {
      case e =>
        Logger.error("ArtistController.isArtistFollowed: ", e)
        InternalServerError
    }
  }
}
