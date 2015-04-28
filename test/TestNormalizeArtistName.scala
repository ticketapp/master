import models.Genre

import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.{JsValue, Json}
import models.Artist._

class TestNormalizeArtistName extends PlaySpec {

  "A sequence of artists names (string)" must {

    "return artists name lowercase" in new App() {
      val artistsName = List("Brassens", "BREL", "Serge gainsbourg", "dutronc")

      val normalizedArtistsName: List[String] = artistsName.map { normalizeArtistName }

      val expectedResult = List("brassens", "brel", "serge gainsbourg", "dutronc")

      normalizedArtistsName mustBe expectedResult
    }

    "return artists name without fanpage, official, officiel, fb, facebook, page" in new App() {
      val artistsName = List("Bukowski Street Team Officiel -  France", "Cookie Monsta Official")

      val normalizedArtistsName: List[String] = artistsName.map { normalizeArtistName }

      val expectedResult = List("bukowski street team -  france", "cookie monsta")

      normalizedArtistsName mustBe expectedResult
    }
  }
}