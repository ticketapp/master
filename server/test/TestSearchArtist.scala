import play.api.libs.json.Json
import services.Utilities
import testsHelper.GlobalApplicationForModels


class TestSearchArtist extends GlobalApplicationForModels with Utilities {

  "SearchArtist" must {

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

      websites.flatMap(artistMethods.normalizeFacebookUrl) mustBe normalizedUrls
    }

    "remove useless words in a SoundCloudUrl (even if it contains uppercase letters)" in {
      val refactoredSoundCloudWebsite1 = artistMethods.removeUselessInSoundCloudWebsite(
        normalizeUrl("https://sounDcloud.com/nina-kraviz/live-at-space-closing-fiesta"))
      val refactoredSoundCloudWebsite2 = artistMethods.removeUselessInSoundCloudWebsite(
        normalizeUrl("sounDcloud.com/nina-kraviz/live-at-space-closing-fiesta"))
      val refactoredSoundCloudWebsite3 = artistMethods.removeUselessInSoundCloudWebsite(
        normalizeUrl("sounDcloud.com/nina-kraviz"))

      refactoredSoundCloudWebsite1 mustBe "soundcloud.com/nina-kraviz"
      refactoredSoundCloudWebsite2 mustBe "soundcloud.com/nina-kraviz"
      refactoredSoundCloudWebsite3 mustBe "soundcloud.com/nina-kraviz"
    }

    "remove nothing in a non-soundCloud website" in {
      val refactoredSoundCloudWebsite = artistMethods.removeUselessInSoundCloudWebsite(
        normalizeUrl("https://facebook.com/nina-kraviz/live-at-space-closing-fiesta"))
      refactoredSoundCloudWebsite mustBe "facebook.com/nina-kraviz/live-at-space-closing-fiesta"
    }

    "aggregate the image path url with its offsets" in {
      artistMethods.aggregateImageAndOffset(Option("imageUrl"), Option(1), Option(200)) mustBe Some("""imageUrl\1\200""")
      artistMethods.aggregateImageAndOffset(Option("imageUrl"), None, Option(200)) mustBe Some("""imageUrl\0\200""")
      artistMethods.aggregateImageAndOffset(Option("imageUrl"), Option(1), None) mustBe Some("""imageUrl\1\0""")
      artistMethods.aggregateImageAndOffset(Option("imageUrl"), None, None) mustBe Some("""imageUrl\0\0""")
    }

    "read a web profiles json response from Soundcloud" in {
      artistMethods.readMaybeFacebookUrl(Json.parse(
        """[{"kind":"web-profile","id":19587164,"service":"facebook","title":null,
          |"url":"http://www.facebook.com/pages/Diane/454634964631595?ref=hl","username":"pages",
          |"created_at":"2013/06/02 14:39:25 +0000"}]""".stripMargin)) mustBe
        Some("facebook.com/pages/diane/454634964631595?ref=hl")
    }
  }
}

