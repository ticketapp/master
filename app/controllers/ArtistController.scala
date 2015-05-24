package controllers

import json.JsonHelper._
import models.Artist.PatternAndArtist
import models._
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.iteratee.{Iteratee, Enumeratee, Enumerator}
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import securesocial.core.Identity
import services.SearchSoundCloudTracks.getSoundCloudTracksForArtist
import services.SearchYoutubeTracks.getYoutubeTracksForArtist
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import scala.concurrent.Future
import scala.util.{Failure, Success}
import java.util.UUID.randomUUID

object ArtistController extends Controller with securesocial.core.SecureSocial {
  def artists = Action { Ok(Json.toJson(Artist.findAll)) }

  def artistsSinceOffsetBy(number: Int, offset: Int) = Action {
    Ok(Json.toJson(Artist.findSinceOffset(number, offset)))
  }

  def artist(artistId: Long) = Action {
    Artist.find(artistId) match {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def artistByFacebookUrl(facebookUrl: String) = Action {
    Artist.findByFacebookUrl(facebookUrl) match {
      case Some(artist) => Ok(Json.toJson(artist))
      case None =>  Ok
    }
  }

  def artistsByGenre(genre: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Artist.findByGenre(genre, numberToReturn, offset)))
  }

  def findArtistsContaining(pattern: String) = Action { Ok(Json.toJson(Artist.findAllContaining(pattern))) }

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
        "genres" -> seq(
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
        ),
        "likes" -> optional(number),
        "country" -> optional(nonEmptyText)
      )(Artist.formApply)(Artist.formUnapply)
    )(Artist.formWithPatternApply)(Artist.formWithPatternUnapply)
  )

  def createArtist = Action { implicit request =>
    try {
      artistBindingForm.bindFromRequest().fold(
        formWithErrors => {
          Logger.error(formWithErrors.errorsAsJson.toString())
          BadRequest(formWithErrors.errorsAsJson)
        },

        patternAndArtist => {
          val artistId = Artist.save(patternAndArtist.artist)
          val artistWithArtistId = patternAndArtist.artist.copy(artistId = artistId)
          val patternAndArtistWithArtistId = PatternAndArtist(patternAndArtist.searchPattern, artistWithArtistId)

          val tracksEnumerator = getArtistTracks(patternAndArtistWithArtistId)
          val toJsonTracks: Enumeratee[Set[Track], JsValue] = Enumeratee.map[Set[Track]]{ tracks => Json.toJson(tracks) }
          val tracksJsonEnumerator = tracksEnumerator &> toJsonTracks

          Future { tracksEnumerator |>> Iteratee.foreach( a => a.map { Track.save }) }

          Ok.chunked(tracksJsonEnumerator)
        }
      )
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  def getArtistTracks(patternAndArtist: PatternAndArtist) = {
    val soundCloudTracksEnumerator = Enumerator.flatten(
      getSoundCloudTracksForArtist(patternAndArtist.artist).map { soundCloudTracks =>
        Artist.addSoundCloudWebsiteIfMissing(soundCloudTracks.headOption, patternAndArtist.artist)
        Enumerator(soundCloudTracks.toSet)
      })

    val youtubeTracksEnumerator =
      getYoutubeTracksForArtist(patternAndArtist.artist, patternAndArtist.searchPattern)

    Enumerator.interleave(soundCloudTracksEnumerator, youtubeTracksEnumerator).andThen(Enumerator.eof)
  }

  def deleteArtist(artistId: Long) = Action {
    Artist.delete(artistId)
    Redirect(routes.Admin.indexAdmin())
  }

  def followArtistByArtistId(artistId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    val userId = request.user.identityId.userId
    Artist.followByArtistId(userId, artistId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"ArtistController.followArtistByArtistId: user with id $userId already follows artist with id $artistId")
        Conflict("This user already follows this artist.")
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"ArtistController.followArtistByArtistId: there is no artist with the id $artistId")
        Conflict("There is no artist with this id.")
      case Failure(unknownException) =>
        Logger.error("ArtistController.followArtistByArtistId", unknownException)
        Status(INTERNAL_SERVER_ERROR)
    }
  }

  def unfollowArtistByArtistId(artistId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    val userId = request.user.identityId.userId
    Artist.unfollowByArtistId(userId, artistId) match {
      case Success(1) =>
        Ok
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the artist (artistId: $artistId) or the artist does not exist.")
        Conflict
      case Failure(unknownException) =>
        Logger.error("ArtistController.followArtistByArtistId", unknownException)
        InternalServerError
    }
  }
  
  def followArtistByFacebookId(facebookId : String) = SecuredAction(ajaxCall = true) { implicit request =>
    val userId = request.user.identityId.userId
    Artist.followByFacebookId(userId, facebookId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(
          s"""ArtistController.followArtistByFacebookId: user with id $userId already follows
             |artist with facebook id $facebookId""".stripMargin)
        Conflict("This user already follows this artist.")
      case Failure(thereIsNoArtistForThisFacebookIdException: ThereIsNoArtistForThisFacebookIdException) =>
        Logger.error(s"ArtistController.followArtistByFacebookId : there is no artist with the facebook id $facebookId")
        Conflict("There is no artist with this id.")
      case Failure(unknownException) =>
        Logger.error("ArtistController.followArtistByFacebookId", unknownException)
        Status(INTERNAL_SERVER_ERROR)
    }
  }

  def getFollowedArtists = UserAwareAction { implicit request =>
    request.user match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(identity: Identity) => Ok(Json.toJson(Artist.getFollowedArtists(identity.identityId)))
    }
  }

  def isArtistFollowed(artistId: Long) = UserAwareAction { implicit request =>
    request.user match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(identity: Identity) => Ok(Json.toJson(Artist.isFollowed(identity.identityId, artistId)))
    }
  }
}
