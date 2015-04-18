package controllers

import java.io.{IOException, FileNotFoundException}
import java.util.Date
import anorm._
import play.api.data._
import play.api.data.Forms._
import play.api.db.DB
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import scala.io.Source
import play.api.mvc.Results._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import play.api.libs.functional.syntax._
import json.JsonHelper._

/*
 follower counts SC
 regarder le next de facebook
 aller chercher les artistes d'une lettre et deux lettres
 enlever 0.1 par lettre en plus au temps de latence à partir de trois
 un seul match echonest : la prendre et lier à la page fb qi à le plus de followers (exemple ibeyi)
 hendrix : bug si getechoSongs recursif
 */

object Test2 extends Controller {
  def test2 = Action {
    /*try {
      DB.withConnection { implicit connection =>
        /*testIfExist("events", "facebookId", event.facebookId) match {
          case true => None
          case false =>*/
        println(event.geographicPoint)
        val geographicPoint = event.geographicPoint.getOrElse("") match {
          case geographicPointPattern(geoPoint) => s"""$geoPoint"""
          case _ => "{geographicPoint}"
        }
        val test: String = "(5.5,1.4)"
        val currentTimestamp: String = "CURRENT_TIMESTAMP"
        SQL(
          s"""SELECT insert({facebookId}, {isPublic}, {isActive}, {name}, {geographicPoint},
             |{description}, {currentTimestamp}, {currentTimestamp}, {ageRestriction})""".stripMargin)
          .on(
            'facebookId -> event.facebookId,
            'isPublic -> event.isPublic,
            'isActive -> event.isActive,
            'name -> event.name,
            'currentTimestamp -> currentTimestamp,
            'geographicPoint -> test,
            'description -> event.description,
            'startTime -> event.startTime,
            'endTime -> event.endTime,
            'ageRestriction -> event.ageRestriction)
          .executeInsert()
    }*/


    Ok("Okay\n")
  }
}

