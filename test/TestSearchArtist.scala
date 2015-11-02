import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._


class TestSearchArtist extends GlobalApplicationForModels {

  "SearchArtistController" must {

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

    "normalize Facebook urls" in {
      val websites = Set(
        "facebook.com/cruelhand",
        "facebook.com/alexsmokemusic",
        "facebook.com/nemo.nebbia",
        "facebook.com/nosajthing",
        "facebook.com/kunamaze",
        "facebook.com/burningdownalaska",
        "facebook.com/diane-454634964631595/timeline",
        "facebook.com/beingasanocean",
        "facebook.com/theoceancollective",
        "facebook.com/woodwireproject",
        "facebook.com/lotfilafaceb",
        "facebook.com/loheem?fref=ts",
        "facebook.com/monoofjapan",
        "facebook.com/fitforakingband",
        "facebook.com/jp-manova",
        "facebook.com/solstafirice",
        "facebook.com/theamityafflictionofficial",
        "facebook.com/defeaterband",
        "facebook.com/musicseptembre?fref=ts",
        "facebook.com/paulatempleofficial",
        "Facebook.com/djvadim",
        "https://www.Facebook.com/djvadim?_rdr")

      val normalizedUrls = Set("fitforakingband",
        "loheem",
        "lotfilafaceb",
        "theamityafflictionofficial",
        "theoceancollective",
        "musicseptembre",
        "nosajthing",
        "burningdownalaska",
        "beingasanocean",
        "solstafirice",
        "cruelhand",
        "alexsmokemusic",
        "diane-454634964631595",
        "woodwireproject",
        "defeaterband",
        "paulatempleofficial",
        "monoofjapan",
        "jp-manova",
        "nemo.nebbia",
        "kunamaze",
        "djvadim",
        "djvadim")

      websites.map{ artistMethods.normalizeFacebookUrl } mustBe normalizedUrls
    }

    "remove useless words in a SoundCloudUrl (even if it contains uppercase letters)" in {
      val refactoredSoundCloudWebsite1 = artistMethods.removeUselessInSoundCloudWebsite(
        utilities.normalizeUrl("https://sounDcloud.com/nina-kraviz/live-at-space-closing-fiesta"))
      val refactoredSoundCloudWebsite2 = artistMethods.removeUselessInSoundCloudWebsite(
        utilities.normalizeUrl("sounDcloud.com/nina-kraviz/live-at-space-closing-fiesta"))
      val refactoredSoundCloudWebsite3 = artistMethods.removeUselessInSoundCloudWebsite(
        utilities.normalizeUrl("sounDcloud.com/nina-kraviz"))

      refactoredSoundCloudWebsite1 mustBe "soundcloud.com/nina-kraviz"
      refactoredSoundCloudWebsite2 mustBe "soundcloud.com/nina-kraviz"
      refactoredSoundCloudWebsite3 mustBe "soundcloud.com/nina-kraviz"
    }

    "remove nothing in a non-soundCloud website" in {
      val refactoredSoundCloudWebsite = artistMethods.removeUselessInSoundCloudWebsite(
        utilities.normalizeUrl("https://facebook.com/nina-kraviz/live-at-space-closing-fiesta"))
      refactoredSoundCloudWebsite mustBe "facebook.com/nina-kraviz/live-at-space-closing-fiesta"
    }

    "aggregate the image path url with its offsets" in {
      artistMethods.aggregateImageAndOffset("imageUrl", Option(1), Option(200)) mustBe """imageUrl\1\200"""
      artistMethods.aggregateImageAndOffset("imageUrl", None, Option(200)) mustBe """imageUrl\0\200"""
      artistMethods.aggregateImageAndOffset("imageUrl", Option(1), None) mustBe """imageUrl\1\0"""
      artistMethods.aggregateImageAndOffset("imageUrl", None, None) mustBe """imageUrl\0\0"""
    }

    "find facebook artists in set of website" in {

      val expectedListArtistNameByFbUrl = List("jp-manova", "nemo.nebbia", "SEPTEMBRE", "loheem", "lotfi", "woodwire",
        "kunamaze", "nosajthing", "paulatemple", "alexsmoke", "diane", "mono", "solstafirice", "theoceancollective",
        "theamityaffliction", "defeaterband", "beingasanocean", "cruelhand", "fitforakingband", "burningdownalaska")

      val expectedListArtistNameByScUrl = List("osunlade", "woodwire", "kuna-maze", "paulatemple", "alexsmoke", "diane")

      val expectedListArtistNameByYtUrl = List("mono", "solstafirice", "theoceancollective", "THE AMITY AFFLICTION",
        "DEFEATER", "beingasanocean", "cruelhand", "FIT FOR A KING", "BURNING DOWN ALASKA")

      val listOfOtherUrls = List("mixcloud.com/la_face_b ", "nosajthing.com", "discogs.com/artist/1156643-lee-holman",
        "discogs.com/artist/2922409-binny-2", "discogs.com/label/447040-clft", "vimeo.com/irwinb")

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
        "facebook.com/paulatempleofficial")

      val expectedArtists = Set("The Amity Affliction", "Kuna Maze", "Being As An Ocean", "Nemo Nebbia", "Fit For A King",
        "Defeater", "BURNING DOWN ALASKA", "Nosaj Thing", "Mono (Japan)", "SÓLSTAFIR", "The Ocean Collective", "LOHEEM"
      ,"woodwire","Paula Temple","septembre", "Alex Smoke","Diane","Cruel Hand","LOTFI")

      whenReady(artistMethods.getFacebookArtistsByWebsites(websites), timeout(Span(5, Seconds))) {

        _.flatten.map{ artist => artist.artist.name } mustBe expectedArtists }
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
      val expectedArtistNames = List("SHXCXCHCXSH","Osúnlade", "LOTFI", "CLFT", "Hein Cooper")

      whenReady(artistMethods.getEventuallyArtistsInEventTitle(title, websites),
        timeout(Span(5, Seconds))) {

        _.map{ artist => artist.artist.name } mustBe expectedArtistNames
      }
    }
  }
}

