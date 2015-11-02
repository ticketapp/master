
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._
import play.api.db.evolutions.Evolutions

class TestNormalizeArtistName extends GlobalApplicationForModels {

  override def beforeAll() = {
    Evolutions.applyEvolutions(databaseApi.database("tests"))
  }

  override def afterAll() = {
    Evolutions.cleanupEvolutions(databaseApi.database("tests"))
  }

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
