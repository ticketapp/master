import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status._
import testsHelper.GlobalApplicationForModelsIntegration


class SearchArtistIntegrationTest extends GlobalApplicationForModelsIntegration {

  "SearchArtist" must {

    "find a sequence of artists on Facebook" in {
      whenReady(artistMethods.getEventuallyFacebookArtists("rone"), timeout(Span(6, Seconds))) { artists =>
        assert(artists.nonEmpty)
      }
    }

    "find Rone (an artist) on Facebook" in {
      whenReady(artistMethods.getFacebookArtistByFacebookUrl("roneofficial"), timeout(Span(6, Seconds))) { artist =>
        assert(artist.isDefined)
      }
    }

    "find facebook artists from a set of website" in {
      val websites = Set(
        "facebook.com/cruelhand",
        "facebook.com/alexsmokemusic",
        "facebook.com/nemo.nebbia",
        "youtube.com/watch?v=t5mhwqwypva",
        "facebook.com/nosajthing",
        "youtube.com/watch?v=0o663ex_5ts",
        "discogs.com/artist/2922409-binny-2",
        "discogs.com/artist/1156643-lee-holman",
        "facebook.com/kunamaze",
        "youtube.com/watch?v=evogdhdpgvw",
        "mixcloud.com/la_face_b",
        "youtube.com/watch?v=jesdtqr3cko",
        "facebook.com/burningdownalaska",
        "facebook.com/diane-454634964631595/timeline",
        "facebook.com/beingasanocean",
        "soundcloud.com/dianecytochrome",
        "nosajthing.com",
        "facebook.com/theoceancollective",
        "soundcloud.com/osunlade",
        "facebook.com/woodwireproject",
        "youtube.com/watch?v=d4cad8sj6gc",
        "youtube.com/watch?v=wpd6foowana",
        "youtube.com/watch?v=r8n8uy5kmvu",
        "facebook.com/lotfilafaceb",
        "soundcloud.com/woodwire",
        "facebook.com/loheem?fref=ts",
        "yorubarecords.net",
        "soundcloud.com/alexsmoke",
        "facebook.com/monoofjapan",
        "vimeo.com/irwinb",
        "facebook.com/fitforakingband",
        "facebook.com/jp-manova",
        "youtube.com/watch?v=at6mnjcy3co",
        "youtube.com/watch?v=yrjooroce1w",
        "facebook.com/solstafirice",
        "discogs.com/label/447040-clft",
        "facebook.com/theamityafflictionofficial",
        "facebook.com/defeaterband",
        "soundcloud.com/kuna-maze",
        "soundcloud.com/paulatemple",
        "facebook.com/musicseptembre?fref=ts",
        "facebook.com/paulatempleofficial",
        "soundcloud.com/combe")


      whenReady(artistMethods.getFacebookArtistsByWebsites(websites), timeout(Span(10, Seconds))) { artists =>
        val artistsName = artists.map(artist => artist.artist.name)

        artistsName should contain  atLeastOneOf ("The Amity Affliction", "Kuna Maze",
          "Being As An Ocean", "Nemo Nebbia", "Fit For A King", "Defeater", "BURNING DOWN ALASKA", "Nosaj Thing",
          "Mono (Japan)", "SÓLSTAFIR", "The Ocean Collective", "LOHEEM", "woodwire","Paula Temple","septembre",
          "Alex Smoke","Diane","Cruel Hand","LOTFI","Combe")
      }
    }

    "return a status OK when getting an artist on facebook" in {
      whenReady(artistMethods.getFacebookArtist("linofficiel"), timeout(Span(5, Seconds))) { facebookResponse =>
        facebookResponse.status mustBe OK
      }
    }

    "find artists in event's title" in {
      val title =
        """DON'T MESS - !!! (CHK CHK CHK) + BALLADUR ENCORE w/ SHXCXCHCXSH live — KANGDING RAY live set —
          |RITUAL djset ENCORE w/ OSUNLADE 3hrs set — LOTFI CLFT MILITIA invite LEE HOLMAN & BINNY TOKYO LEGENDS
          |#1 Feat. SHUYA OKINO & JUN MATUOKA SOPHIE HUNGER + MARTIN MEY + HEIN COOPER""".stripMargin
      val websites = Set(
        "facebook.com/lotfilafaceb",
        "mixcloud.com/la_face_b",
        "yorubarecords.net",
        "soundcloud.com/osunlade",
        "discogs.com/artist/1156643-lee-holman",
        "discogs.com/artist/2922409-binny-2",
        "discogs.com/label/447040-clft")

      whenReady(artistMethods.getEventuallyArtistsInEventTitle(title, websites), timeout(Span(20, Seconds))) {

        _.map(_.artist.name) should contain atLeastOneOf
          ("SHXCXCHCXSH", "Kangding Ray", "Osúnlade", "Osunlade", "LOTFI", "CLFT", "Hein Cooper")
      }
    }

    "find artists for an event" in {
      val artistName = "LOTFI"
      val websites = Set(
        "facebook.com/lotfilafaceb",
        "mixcloud.com/la_face_b",
        "yorubarecords.net",
        "soundcloud.com/osunlade",
        "discogs.com/artist/1156643-lee-holman",
        "discogs.com/artist/2922409-binny-2",
        "discogs.com/label/447040-clft")
      val expectedArtistNames = List("LOTFI")

      whenReady(artistMethods.getArtistsForAnEvent(artistName, websites), timeout(Span(20, Seconds))) {

        _.map{ artist => artist.artist.name } should contain theSameElementsAs expectedArtistNames
      }
    }

    "find facebook artists for a string" in {
      val artistName = "LOTFI CLFT"
      val websites = Set(
        "facebook.com/lotfilafaceb",
        "mixcloud.com/la_face_b",
        "yorubarecords.net",
        "soundcloud.com/osunlade",
        "discogs.com/artist/1156643-lee-holman",
        "discogs.com/artist/2922409-binny-2",
        "discogs.com/label/447040-clft")
      val expectedArtistNames = List("LOTFI", "CLFT")

      whenReady(artistMethods.getFacebookArtist(artistName, websites),
        timeout(Span(20, Seconds))) {

        _.map{ artist => artist.artist.name } should contain theSameElementsAs expectedArtistNames
      }
    }

    "return a status OK when getting a facebook url on Soundcloud" in {
      whenReady(artistMethods.getFacebookUrlBySoundCloudUrl("soundcloud.com/osunlade"),
        timeout(Span(5, Seconds))) { soundCloudResponse =>

        soundCloudResponse.status mustBe OK
      }
    }
  }
}
