package services

import models.Artist
import org.specs2.mutable._
import play.api.libs.json.{JsValue, Json}

import play.api.test._
import play.api.test.Helpers._

class SearchSoundCloudTracksTest extends Specification {
  "SearchSoundCloudTracks" should {

    "find tracks for manu chao" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {

        val json: JsValue = Json.parse(
          """
{
  "user": {
    "name" : "toto",
    "age" : 25,
    "email" : "toto@jmail.com",
    "isAlive" : true,
    "friend" : {
  	  "name" : "tata",
  	  "age" : 20,
  	  "email" : "tata@coldmail.com"
    }
  }
}
                                       """)

        val artist = Artist(
            artistId =5l,
            facebookId = Some("facebook id"),
            name = "name",
            description = Some("description"),
            facebookUrl = "facebook url",
            websites = Set("website"),
            images = Set.empty,
            genres = Set.empty,
            tracks = Seq.empty
        )
        val result = SearchSoundCloudTracks.readSoundCloudTracks(json, artist)

        print(result)
      }
    }
  }
}
