import json.JsonHelper._
import models.{ArtistMethods, GenreMethods, TrackMethods}
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}

import scala.language.postfixOps

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