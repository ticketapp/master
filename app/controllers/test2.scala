package controllers

import java.io.{IOException, FileNotFoundException}
import java.util.Date
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Place, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Failure, Success, Try}
import play.api.libs.functional.syntax._
import json.JsonHelper._
import services.Utilities.normalizeString

/*
 follower counts SC
 regarder le next de facebook
 aller chercher les artistes d'une lettre et deux lettres
 enlever 0.1 par lettre en plus au temps de latence à partir de trois
 un seul amtch echonest : la prendre et lier à la page fb qi à le plus de followers (exemple ibeyi)
 array vide retournés?
 hendrix : bug si getechoSongs recursif
 */

object Test2 extends Controller {
  def test2 = Action {
    Ok("Okay\n")
  }
}

