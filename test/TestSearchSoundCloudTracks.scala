/*
import java.util.UUID

>>>>>>> master
import models.{Artist, Track}
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._

class TestSearchSoundCloudTracks extends PlaySpec with OneAppPerSuite {

  val ninaKraviz = Artist(None, Option("facebookId3"), "nina", Option("imagePath"), Option("description"),
    "facebookUrl3", Set("soundcloud.com/nina-kraviz"))
  val worakls = Artist(None, Option("facebookId3"), "worakls", Option("imagePath"), Option("description"),
    "worakls", Set.empty)

  "SearchSoundCloudTracks" must {

    "find tracks on SoundCloud" in {
      whenReady(getSoundCloudTracksForArtist(ninaKraviz), timeout(Span(3, Seconds))) { tracks =>
        tracks should not be empty
      }
    }

    "find soundCloud ids for artist name worakls" in {
      whenReady(getSoundCloudIdsForName("worakls"), timeout(Span(2, Seconds))) { soundCloudIds =>
        soundCloudIds should contain allOf (68442, 4329372, 13302835)
      }
    }

    "find soundCloud websites for a list of soundCloud ids" in {
      whenReady(getSoundcloudWebsites(List(68442, 4329372, 13302835, 97091845, 129311935, 366396)),
        timeout(Span(2, Seconds))) { tupleSoundCloudIdWebsites =>
        tupleSoundCloudIdWebsites should contain
        (68442, Seq("hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls", "facebook.com/worakls"))
      }
    }

    "compute the confidence of an souncloud with websites" in {
      val artist = Artist(Some(1), Option("100297159501"), "worakls", Option("imagePath"), Option("description"),
        "worakls", Set("hungrymusic.fr",  "youtube.com/user/worakls/videos", "twitter.com/worakls"))
      val soundCloudId1 = 124
      val soundCloudId2 = 1234
      val soundCloudId3 = 12345
      val listWebsitesSc1 = Seq("hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls",
        "facebook.com/worakls")
      val listWebsitesSc2 = Seq("facebook.com/pages/nto/50860802143", "hungrymusic.fr", "youtube.com/user/ntotunes",
        "twitter.com/#!/ntohungry")
      val listWebsitesSc3 = Seq("hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls")
      computationScConfidence(artist, listWebsitesSc1, soundCloudId1) mustBe SoundCloudArtistConfidence(Some(1), 124, 1.0.toFloat)
      computationScConfidence(artist, listWebsitesSc2, soundCloudId2) mustBe SoundCloudArtistConfidence(Some(1), 1234, 0.07826595210746258.toFloat)
      computationScConfidence(artist, listWebsitesSc3, soundCloudId3) mustBe SoundCloudArtistConfidence(Some(1), 12345, 0.5258055254220182.toFloat)
    }

    "save soundcloud websites for an artist" in {
      val track = Track(UUID.fromString("9a9ca254-0245-4a69-b66c-494f3a0ced3e"),"Toi (Snippet)",
      "https://api.soundcloud.com/tracks/190465678/stream",'s',
      "https://i1.sndcdn.com/artworks-000106271172-2q3z78-large.jpg","worakls","Worakls",
      Some("http://soundcloud.com/worakls/toi-snippet"),None,None,List())
      val artist = Artist(Option(26.toLong), Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
        Option("description"), "facebookUrl", Set("website"))
      whenReady(addSoundCloudWebsitesIfNotInWebsites(Some(track), artist), timeout(Span(6, Seconds))) {
        _ mustBe List("http://www.hungrymusic.fr", "https://www.youtube.com/user/worakls/videos",
          "https://twitter.com/worakls", "https://www.facebook.com/worakls/")
      }
    }
  }
}
*/
