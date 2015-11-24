import org.scalatest.Matchers._
import org.scalatestplus.play._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.language.postfixOps


class TestSearchTracksController extends PlaySpec with OneAppPerSuite {

  "SearchTracksController" must {

    val tupleTitleArtistNameReads =
      Reads.seq((__ \ "title").read[String] and (__ \ "artistName").read[String] tupled)

    "find tracks on Youtube by a title and an artist" in {
      val eventuallyResult = route(
        FakeRequest(
          tracksDomain.routes.SearchTracksController
            .getYoutubeTracksForArtistAndTrackTitle("brassens", "facebookUrlSearchTracksController", "pauvre martin"))
      ).get

      status(eventuallyResult) mustBe 200

      val seqTitleArtistName = contentAsJson(eventuallyResult).as[Seq[(String, String)]](tupleTitleArtistNameReads)

      seqTitleArtistName should not be empty
      seqTitleArtistName.toSet.size mustBe seqTitleArtistName.size
    }

    "get youtube track info" in {
      val Some(info) = route(FakeRequest(tracksDomain.routes.SearchTracksController.getYoutubeTrackInfo("ujUUkrmfpis")))

      contentAsString(info) should include("author=Pan-Pot")
    }
  }
}