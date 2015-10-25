import java.util.UUID

import models._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import services.SoundCloudArtistConfidence
import org.scalatest.Matchers._


class TestSearchSoundCloudTracks extends PlaySpec with OneAppPerSuite with Injectors {

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

    "get SoundCLoud tracks without SoundCloud website for Worakls" in {
      whenReady(searchSoundCloudTracks.getSoundCloudTracksWithoutSoundCloudWebsite(worakls), timeout(Span(3, Seconds))) { tracks =>
        val uuid = UUID.randomUUID
        tracks map (_.copy(uuid = uuid)) should contain (Track(
          uuid = uuid,
          title = "Toi (Snippet)",
          url = "https://api.soundcloud.com/tracks/190465678/stream",
          platform = 's',
          thumbnailUrl = "https://i1.sndcdn.com/artworks-000106271172-2q3z78-large.jpg",
          artistFacebookUrl = "worakls",
          artistName = "worakls",
          redirectUrl = Some("http://soundcloud.com/worakls/toi-snippet"),
          confidence = 0.0))
      }
    }

    "return SoundCLoud accounts with the best confidence" in {
      val soundCloudArtistConfidence0 = SoundCloudArtistConfidence(artistId = None, soundcloudId = 1, confidence = 0.0)
      val soundCloudArtistConfidence1 = SoundCloudArtistConfidence(artistId = None, soundcloudId = 1, confidence = 3.0)
      val soundCloudArtistConfidence2 = SoundCloudArtistConfidence(artistId = None, soundcloudId = 1, confidence = 1.0)
      val soundCloudArtistConfidence3 = SoundCloudArtistConfidence(artistId = None, soundcloudId = 2, confidence = 3.0)
      val accountsWithBestConfidence = searchSoundCloudTracks.returnSoundCloudAccountsWithBestConfidence(Seq(
        soundCloudArtistConfidence0, soundCloudArtistConfidence1, soundCloudArtistConfidence2, soundCloudArtistConfidence3))

      accountsWithBestConfidence should contain only (soundCloudArtistConfidence1, soundCloudArtistConfidence3)
    }

    "not return SoundCloud accounts id the best confidence is 0" in {
      val soundCloudArtistConfidence = SoundCloudArtistConfidence(artistId = None, soundcloudId = 1, confidence = 0.0)

      searchSoundCloudTracks.returnSoundCloudAccountsWithBestConfidence(
        Seq(soundCloudArtistConfidence, soundCloudArtistConfidence)) mustBe Seq.empty
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
      searchSoundCloudTracks.computeSoundCloudConfidence(artist, listWebsitesSc1, soundCloudId1) mustBe
        SoundCloudArtistConfidence(Some(1), 124, 1.0)
      searchSoundCloudTracks.computeSoundCloudConfidence(artist, listWebsitesSc2, soundCloudId2) mustBe
        SoundCloudArtistConfidence(Some(1), 1234, 0.07826595210746258)
      searchSoundCloudTracks.computeSoundCloudConfidence(artist, listWebsitesSc3, soundCloudId3) mustBe
        SoundCloudArtistConfidence(Some(1), 12345, 0.5258055254220182)
    }
  }
}
