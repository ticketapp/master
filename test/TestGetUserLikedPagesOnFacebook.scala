import play.api.libs.json.Json

class TestGetUserLikedPagesOnFacebook extends GlobalApplicationForModels {

  "A GetUserLikedPagesOnFacebook service" must {

    val facebookResponse = Json.parse(
      """
        |{
        |  "id": "522060427930119",
        |  "name": "Lo Del",
        |  "events": {
        |    "data": [
        |      {
        |        "owner": {
        |          "name": "Festival Demon d'or",
        |          "id": "370548142976713"
        |        },
        |        "admins": {
        |          "data": [
        |            {
        |              "id": "370548142976713",
        |              "name": "Festival Demon d'or"
        |            }
        |          ],
        |          "paging": {
        |            "cursors": {
        |              "before": "MzcwNTQ4MTQyOTc2NzEz",
        |              "after": "MzcwNTQ4MTQyOTc2NzEz"
        |            }
        |          }
        |        },
        |        "id": "1385294928455172"
        |      },
        |      {
        |        "owner": {
        |          "name": "Martin Borough",
        |          "id": "10153063311986684"
        |        },
        |        "admins": {
        |          "data": [
        |            {
        |              "id": "10153063311986684",
        |              "name": "Martin Borough"
        |            },
        |            {
        |              "id": "10206612348529238",
        |              "name": "Olivier"
        |            }
        |          ],
        |          "paging": {
        |            "cursors": {
        |              "before": "MTAxNTMwNjMzMTE5ODY2ODQZD",
        |              "after": "MTAyMDY2MTIzNDg1MjkyMzgZD"
        |            }
        |          }
        |        },
        |        "id": "363228570542526"
        |      }
        |    ],
        |    "paging": {
        |      "cursors": {
        |        "before": "TVRNNE5USTVORGt5T0RRMU5URTNNam94TkRNMU16TXdPREF3T2pFMk5UQTRORGc1TmpnME9EVTRNUT09",
        |        "after": "TVRnM016WXhOakkwTnpZAMU5UWXlPakV6T0RBME1USTRNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD"
        |      },
        |      "next": "https://graph.facebook.com/v2.5/522060427930119/events?access_token=CAACEdEose0cBAJeyZBnB3ZBTJlUWEjG5Q2RPLDdKTgFHxNEUnxHDGBBXj4SXGfb0zn8jMghEVNhG4qCAkVVBPZAWIgWHXHYbMfW0V3swFtEyUledVSVNhcZB9SO0IBxoTuueSnsPSr6dEfxuu2gxDZB5kudn6iBiQiIJY8QPSYjZCacZAP283HzuQadXircVdLYT4tx0wHhPGma5dd78ZB6k&pretty=0&fields=owner%2Cadmins&limit=25&after=TVRnM016WXhOakkwTnpZAMU5UWXlPakV6T0RBME1USTRNREE2TVRZAMU1EZAzBPRGsyT0RRNE5UZA3gZD"
        |    }
        |  },
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

    "find events Ids from Json facesbook response" in {
      val expectedIds = Seq("1385294928455172", "363228570542526")
      getUserLikedPagesOnFacebook.findEventsIds(facebookResponse) mustBe expectedIds
    }

    "transform facebook page to page tuple" in {
      val expectedTuples = Vector(
        ("887772324613065",Some("Concert Tour")),
        ("1499505843697664",Some("Non-Profit Organization")),
        ("136112080076799",Some("Arts/Entertainment/Nightlife")),
        ("869259956491158",Some("Arts/Entertainment/Nightlife")),
        ("534079613309595",Some("Musician/Band"))
      )
      getUserLikedPagesOnFacebook.facebookPageToPageTuple(facebookResponse) mustBe expectedTuples
    }
  }
}

