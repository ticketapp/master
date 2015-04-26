import models.Genre

import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.{JsValue, Json}
import models.Artist._

class NormalizeArtistNameTest extends PlaySpec {

  "A sequence of artists names (string)" must {

    "return artists name lowercase" in {
      val artistsName = List("Brassens", "BREL", "Serge gainsbourg", "dutronc")

      val normalizedArtistsName: List[String] = artistsName.map { normalizeArtistName }

      val expectedResult = List("brassens", "brel", "serge gainsbourg", "dutronc")

      normalizedArtistsName mustBe expectedResult
    }

    /*"return artists name without fanpage, official, officiel, fb, facebook, page" in {
      val artistsName = List("", None)

      val genresSets: List[Set[Genre]] = genres.map {
        genresStringToGenresSet
      }

      val expectedResult = List(Set.empty, Set.empty)

      genresSets mustBe expectedResult
    }*/
  }
}