package controllers

import anorm._
import models.Artist
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.DB
import play.api.mvc._
import play.api.libs.json.Json


object ArtistController extends Controller {
  def artists = Action {
    Ok(Json.toJson(Artist.findAll()))
  }

  def artist(artistId: Long) = Action {
    Ok(Json.toJson(Artist.find(artistId)))
  }

  def artistsStartingWith(pattern: String) = Action {
    Ok(Json.toJson(Artist.findAllStartingWith(pattern)))
  }

  val artistBindingForm = Form(mapping(
    "facebookId" -> longNumber(),
    "artistName" -> nonEmptyText(2)
  )(Artist.formApply)(Artist.formUnapply)
  )

  def createArtist = Action { implicit request =>
    artistBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      artist => {
        Ok(Json.toJson(Artist.saveArtist(artist)))
      }
    )
  }

  def deleteArtist(artistId: Long) = Action {
    Artist.deleteArtist(artistId)
    Redirect(routes.Admin.indexAdmin())
  }

  def followArtist(userId : Long, artistId : Long) = Action {
    Artist.followArtist(userId, artistId)
    Redirect(routes.Admin.indexAdmin())
  }
}
