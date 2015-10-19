
import models.{ArtistMethods, GenreMethods, TrackMethods}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}

class TestNormalizeArtistName extends PlaySpec with OneAppPerSuite {


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

  "A sequence of artists names (strings)" must {

    "return artists name lowercase" in {
      val artistsName = List("Brassens", "BREL", "Serge gainsbourg", "dutronc")

      val normalizedArtistsName: List[String] = artistsName.map { artistMethods.normalizeArtistName }

      val expectedResult = List("brassens", "brel", "serge gainsbourg", "dutronc")

      normalizedArtistsName mustBe expectedResult
    }

    "return artists name trimmed without multiple spaces and tabs" in {
      val artistsName = List(" abc ", "ab  cd", "ab cd")

      val normalizedArtistsName: List[String] = artistsName.map { artistMethods.normalizeArtistName }

      val expectedResult = List("abc", "ab cd", "ab cd")

      normalizedArtistsName mustBe expectedResult
    }

    "return artists name without fanpage, official, officiel, fb, facebook, page" in {
      val artistsName = List("Bukowski Street Team Officiel -  France", "Cookie Monsta Official")

      val normalizedArtistsName: List[String] = artistsName.map { artistMethods.normalizeArtistName }

      val expectedResult = List("bukowski street team - france", "cookie monsta")

      normalizedArtistsName mustBe expectedResult
    }
  }
}
