import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers


class TestSearchTracksController extends GlobalApplicationForControllers {
  sequential

  "SearchTracksController" should {

    "find tracks on Youtube by a title and an artist" in {
      val tupleTitleArtistNameReads =
        Reads.seq((__ \ "title").read[String] and (__ \ "artistName").read[String] tupled)

      val Some(result) = route(FakeRequest(
        tracksDomain.routes.SearchTracksController
          .getYoutubeTracksForArtistAndTrackTitle("brassens", "facebookUrlSearchTracksController", "pauvre martin")))

      status(result) mustEqual 200

      val seqTitleArtistName = contentAsJson(result).as[Seq[(String, String)]](tupleTitleArtistNameReads)

      seqTitleArtistName should not be empty
      seqTitleArtistName.toSet.size mustEqual seqTitleArtistName.size
    }

    "get youtube track info" in {
      val Some(info) = route(FakeRequest(tracksDomain.routes.SearchTracksController.getYoutubeTrackInfo("ujUUkrmfpis")))

      contentAsString(info) must contain("author=Pan-Pot")
    }
  }
}