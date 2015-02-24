package controllers

import json.JsonHelper._
import models.Artist
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import services.Utilities.normalizeString


object ArtistController extends Controller with securesocial.core.SecureSocial {
  def artists = Action {
    Ok(Json.toJson(Artist.findAll()))
  }

  def artist(artistId: Long) = Action {
    Ok(Json.toJson(Artist.find(artistId)))
  }

  def findArtistsContaining(pattern: String) = Action {
    Ok(Json.toJson(Artist.findAllContaining(pattern)))
  }


  val artistBindingForm = Form( mapping(
    "facebookId" -> optional(nonEmptyText(2)),
    "artistName" -> nonEmptyText(2)
  )(Artist.formApply)(Artist.formUnapply)
  )

  def createArtist = Action { implicit request =>
    try {
      artistBindingForm.bindFromRequest().fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        artist => {
          Ok(Json.toJson(Artist.save(artist)))
        }
      )
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  def deleteArtist(artistId: Long) = Action {
    Artist.deleteArtist(artistId)
    Redirect(routes.Admin.indexAdmin())
  }

  def followArtist(artistId : Long) = Action {
    //Artist.followArtist(userId, artistId)
    Redirect(routes.Admin.indexAdmin())
  }



}
