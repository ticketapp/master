
import java.util.UUID
import models._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SoundCloudArtistConfidence, SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}

class TestSearchSoundCloudTracks extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)

  val ninaKraviz = Artist(None, Option("facebookId3"), "nina", Option("imagePath"), Option("description"),
    "facebookUrl3", Set("soundcloud.com/nina-kraviz"))
  val worakls = Artist(None, Option("facebookId3"), "worakls", Option("imagePath"), Option("description"),
    "worakls", Set.empty)

  "SearchSoundCloudTracks" must {

    "find tracks on SoundCloud" in {
      whenReady(searchSoundCloudTracks.getSoundCloudTracksForArtist(ninaKraviz), timeout(Span(3, Seconds))) { tracks =>
        assert(tracks.nonEmpty)
      }
    }

    "find soundCloud ids for artist name worakls" in {
      whenReady(searchSoundCloudTracks.getSoundCloudIdsForName("worakls"), timeout(Span(2, Seconds))) { soundCloudIds =>
        soundCloudIds must contain allOf (68442, 4329372, 13302835)
      }
    }

    "find soundCloud websites for a list of soundCloud ids" in {
      whenReady(searchSoundCloudTracks.getSoundcloudWebsites(List(68442, 4329372, 13302835, 97091845, 129311935, 366396)),
        timeout(Span(2, Seconds))) { tupleSoundCloudIdWebsites =>
        tupleSoundCloudIdWebsites must contain
        (68442, Seq("hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls", "facebook.com/worakls"))
      }
    }

    "compute the confidence of an souncloud with websites" in {
      val artist = Artist(Some(1), Option("100297159501"), "worakls", Option("imagePath"), Option("description"),
        "worakls", Set("hungrymusic.fr",  "youtube.com/user/worakls/videos", "twitter.com/worakls"))
      val soundCloudId1 = 124L
      val soundCloudId2 = 1234L
      val soundCloudId3 = 12345L
      val listWebsitesSc1 = Seq("hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls",
        "facebook.com/worakls")
      val listWebsitesSc2 = Seq("facebook.com/pages/nto/50860802143", "hungrymusic.fr", "youtube.com/user/ntotunes",
        "twitter.com/#!/ntohungry")
      val listWebsitesSc3 = Seq("hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls")
      searchSoundCloudTracks.computationScConfidence(artist, listWebsitesSc1, soundCloudId1) mustBe
        SoundCloudArtistConfidence(Some(1), 124, 1.0)
      searchSoundCloudTracks.computationScConfidence(artist, listWebsitesSc2, soundCloudId2) mustBe
        SoundCloudArtistConfidence(Some(1), 1234, 0.07826595210746258)
      searchSoundCloudTracks.computationScConfidence(artist, listWebsitesSc3, soundCloudId3) mustBe
        SoundCloudArtistConfidence(Some(1), 12345, 0.5258055254220182)
    }

    "save soundcloud websites for an artist" in {
      val track = Track(UUID.fromString("9a9ca254-0245-4a69-b66c-494f3a0ced3e"),"Toi (Snippet)",
      "https://api.soundcloud.com/tracks/190465678/stream",'s',
      "https://i1.sndcdn.com/artworks-000106271172-2q3z78-large.jpg","worakls","Worakls",
      Some("http://soundcloud.com/worakls/toi-snippet"),None/*,None,List()*/)
      val artist = Artist(Option(26.toLong), Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
        Option("description"), "facebookUrl", Set("website"))
      whenReady(artistMethods.addSoundCloudWebsitesIfNotInWebsites(Some(track), artist), timeout(Span(6, Seconds))) {
        _ mustBe List("http://www.hungrymusic.fr", "https://www.youtube.com/user/worakls/videos",
          "https://twitter.com/worakls", "https://www.facebook.com/worakls/")
      }
    }
  }
}
