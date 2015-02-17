package controllers

import java.util.Date

import models.Artist
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.ws.{Response, WS}
import play.api.mvc._
import play.api.libs.json.{JsValue, Json}
import services.Utilities
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.util.{Failure, Success}

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

  val token = play.Play.application.configuration.getString("facebook.token")

  def returnFacebookPages(pattern: String): Future[JsValue] = {
    WS.url("https://graph.facebook.com/v2.2/search?q=" + pattern
      + "&limit=400&type=page&fields=name,cover,id,category,likes,link,website&access_token=" + token).get.map {
      response => response.json
    }
  }

  def findFacebookArtistsContaining(pattern: String) = Action.async {
    returnFacebookPages(pattern).map {
      resp => Ok(resp)
    }
      /*.map {
      pages => Ok(Json.toJson(Seq(new Artist(-1L, new Date(), Some(""), "", Some(""), List(), List(), List()))))*/
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

  def followArtist(userId : Long, artistId : Long) = Action {
    Artist.followArtist(userId, artistId)
    Redirect(routes.Admin.indexAdmin())
  }
}
