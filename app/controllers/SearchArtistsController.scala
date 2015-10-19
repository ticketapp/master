package controllers

import javax.inject.Inject
import play.api.Play.current
import json.JsonHelper._
import models.{ArtistMethods, GenreMethods, Artist, AddressMethods}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.ws.{WSResponse, WS}
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import services.Utilities
import play.api.libs.json.Reads._

import scala.language.postfixOps
import scala.util.matching.Regex

class SearchArtistsController @Inject()(dbConfigProvider: DatabaseConfigProvider,
                                        val addressMethods: AddressMethods,
                                        val artistMethods: ArtistMethods,
                                        val genreMethods: GenreMethods,
                                        val utilities: Utilities) extends Controller {

  val facebookArtistFields = "name,cover{source,offset_x,offset_y},id,category,link,website,description,genre,location,likes"

  def getFacebookArtistsContaining(pattern: String) = Action.async {
    artistMethods.getEventuallyFacebookArtists(pattern).map { artists =>
      Ok(Json.toJson(artists))
    }
  }
}
