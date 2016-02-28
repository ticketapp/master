import play.api.libs.json.Json
import services.PageIdAndCategory
import testsHelper.GlobalApplicationForModels


class TestGetUserLikedPagesOnFacebook extends GlobalApplicationForModels {

  "GetUserLikedPagesOnFacebook service" must {

    "transform a facebook page to a sequence of PageIdAndCategory" in {
      val facebookResponse = Json.parse(
        """
          |{
          |  "id": "522060427930119",
          |  "name": "Lo Del",
          |  "likes": {
          |    "data": [
          |      {
          |        "id": "887772324613065",
          |        "name": "I Love Techno Europe",
          |        "category": "Concert Tour"
          |      }
          |    ],
          |    "paging": {
          |      "cursors": {
          |        "before": "ODg3NzcyMzI0NjEzMDY1",
          |        "after": "OTE2NzIzOTExNjczMDM1"
          |      }
          |    }
          |  }
          |}
        """.stripMargin)

      val expectedTuples = Vector(
        PageIdAndCategory("887772324613065",Some("Concert Tour"))
      )

      getUserLikedPagesOnFacebook.facebookPageToPageIdAndCategory(facebookResponse) mustBe expectedTuples
    }
  }
}

