import java.util.Date
import controllers.DAOException
import models.{Track, Place, Artist, Event}
import models.Track._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json._
import play.api.test.{WithApplication, PlaySpecification, FakeRequest}
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._

import play.api.Play.current

import scala.language.postfixOps
import scala.util.Success
import scala.util.Failure
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test._
import play.api.test.Helpers._
import json.JsonHelper._
import play.api.libs.functional.syntax._

class TestSearchTracksController extends PlaySpec with OneAppPerSuite {

  "SearchTracksController" must {

    val tupleTitleArtistNameReads =
      Reads.seq((__ \ "title").read[String] and (__ \ "artistName").read[String] tupled)

    "find tracks on Youtube by a title and an artist" in {
//      val artist = Artist(None, None, "brassens", None, None, "facebookUrlSearchTracksController", Set("website"))
      val eventuallyResult = route(
        FakeRequest(
          controllers.routes.SearchTracksController
            .getYoutubeTracksForArtistAndTrackTitle("brassens", "facebookUrlSearchTracksController", "pauvre martin"))
      ).get

      status(eventuallyResult) mustBe 200

      val seqTitleArtistName = contentAsJson(eventuallyResult).as[Seq[(String, String)]](tupleTitleArtistNameReads)
      println(seqTitleArtistName)
      seqTitleArtistName should not be empty
      seqTitleArtistName.toSet.size mustBe seqTitleArtistName.size
    }
  }
}