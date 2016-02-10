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
          |      },
          |      {
          |        "id": "1499505843697664",
          |        "name": "Nuitssonores",
          |        "category": "Non-Profit Organization"
          |      },
          |      {
          |        "id": "136112080076799",
          |        "name": "Toï Toï le Zinc",
          |        "category": "Arts/Entertainment/Nightlife"
          |      },
          |      {
          |        "id": "869259956491158",
          |        "name": "Axiome",
          |        "category": "Arts/Entertainment/Nightlife"
          |      },
          |      {
          |        "id": "534079613309595",
          |        "name": "Mégafaune",
          |        "category": "Musician/Band"
          |      }
          |    ],
          |    "paging": {
          |      "cursors": {
          |        "before": "ODg3NzcyMzI0NjEzMDY1",
          |        "after": "OTE2NzIzOTExNjczMDM1"
          |      },
          |      "next": "https://graph.facebook.com/v2.5/522060427930119/likes?access_token=CAACEdEose0cBAJeyZBnB3ZBTJlUWEjG5Q2RPLDdKTgFHxNEUnxHDGBBXj4SXGfb0zn8jMghEVNhG4qCAkVVBPZAWIgWHXHYbMfW0V3swFtEyUledVSVNhcZB9SO0IBxoTuueSnsPSr6dEfxuu2gxDZB5kudn6iBiQiIJY8QPSYjZCacZAP283HzuQadXircVdLYT4tx0wHhPGma5dd78ZB6k&pretty=0&fields=id%2C+name%2C+category%2C+categories_list&limit=25&after=OTE2NzIzOTExNjczMDM1"
          |    }
          |  }
          |}
        """.stripMargin)

      val expectedTuples = Vector(
        PageIdAndCategory("887772324613065",Some("Concert Tour")),
        PageIdAndCategory("1499505843697664",Some("Non-Profit Organization")),
        PageIdAndCategory("136112080076799",Some("Arts/Entertainment/Nightlife")),
        PageIdAndCategory("869259956491158",Some("Arts/Entertainment/Nightlife")),
        PageIdAndCategory("534079613309595",Some("Musician/Band"))
      )

      getUserLikedPagesOnFacebook.facebookPageToPageIdAndCategory(facebookResponse) mustBe expectedTuples
    }
  }
}

