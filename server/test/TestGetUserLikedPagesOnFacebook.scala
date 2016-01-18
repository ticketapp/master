import java.util.UUID

import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.libs.json.Json
import testsHelper.GlobalApplicationForModels

import services.PageIdAndCategory


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

    "save artists from facebook and make the relation with an user" in {
      val facebookArtists = Vector(
        PageIdAndCategory("534079613309595", Option("Musician/Band")),
        PageIdAndCategory("493205657502998", Option("Musician/Band")),
        PageIdAndCategory("175007802512911", Option("Musician/Band")),
        PageIdAndCategory("198374666900337", Option("Musician/Band")),
        PageIdAndCategory("916723911673035", Option("Musician/Band")),
        PageIdAndCategory("312698145585982", Option("Musician/Band")),
        PageIdAndCategory("144703482207721", Option("Musician/Band")),
        PageIdAndCategory("546377438806185", Option("Musician/Band")),
        PageIdAndCategory("212419688422", Option("Musician/Band")),
        PageIdAndCategory("50860802143", Option("Musician/Band")),
        PageIdAndCategory("36511744012", Option("Musician/Band")),
        PageIdAndCategory("192110944137172", Option("Musician/Band")),
        PageIdAndCategory("395337121981", Option("Musician/Band")))

      val userUuid = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")

      val expectedFacebookIds = Set(Some("534079613309595"), Some("493205657502998"), Some("175007802512911"),
        Some("198374666900337"), Some("916723911673035"), Some("312698145585982"), Some("144703482207721"),
        Some("546377438806185"), Some("212419688422"), Some("50860802143"), Some("36511744012"),
        Some("192110944137172"), Some("395337121981"))


      whenReady(getUserLikedPagesOnFacebook.makeRelationArtistUserOneByOne(facebookArtists, userUuid),
        timeout(Span(240, Seconds))) { isSavedWithRelation =>

        isSavedWithRelation mustBe true

        whenReady(artistMethods.findAll, timeout(Span(5, Seconds))) { artists =>

          artists map (_.facebookId) must contain allOf(Some("534079613309595"), Some("493205657502998"),
            Some("175007802512911"), Some("198374666900337"), Some("916723911673035"), Some("312698145585982"),
            Some("144703482207721"), Some("546377438806185"), Some("212419688422"), Some("50860802143"),
            Some("36511744012"), Some("192110944137172"), Some("395337121981"))
        }

        whenReady(artistMethods.getFollowedArtists(userUuid), timeout(Span(5, Seconds))) { followedArtists =>
          followedArtists map (_.artist.facebookId) must contain theSameElementsAs expectedFacebookIds
        }
      }
    }
  }
}

