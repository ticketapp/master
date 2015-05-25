import services.Utilities._
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.{JsValue, Json}
import models.Artist._

class TestUtilities extends PlaySpec with OneAppPerSuite {

  "A utilities" must {

    "normalize string with the method normalizeString" in {
      val strings = List("éh'=)àç_è-(aék", "abc de#f")

      val normalizedString: List[String] = strings.map { normalizeString }

      val expectedResult = List("éh'=)àç_è-(aék", "abc de#f")

      normalizedString mustBe expectedResult
    }
  }

  "normalize urls" in {
    val urls = Seq("abc.fr", "www.abc.com", "http://cde.org", "https://jkl.wtf", "http://www.claude.cool",
      "https://www.claude.music")

    val expectedUrls = Seq("abc.fr", "abc.com", "cde.org", "jkl.wtf", "claude.cool", "claude.music")

    val normalizedUrls = urls map { normalizeUrl }

    normalizedUrls mustBe expectedUrls
  }

  "create a new instance of GeographicPoints" in {
    GeographicPoint("(0,0)")
    GeographicPoint("(0.4,0)")
    GeographicPoint("(0.4,0.5784)")
    GeographicPoint("(9,0.5784)")
    GeographicPoint("(-9,0.5784)")
    GeographicPoint("(-9,-0.5784)")
    GeographicPoint("(9,-0.5784)")
    GeographicPoint("(-48.87965412354687,-145.5754545484)")
  }

  "throw exceptions while instantiating these geographicPoints" in {
    an [IllegalArgumentException] should be thrownBy GeographicPoint("0,0")
  }

  "refactor events or places names" in {
    val eventsName = Seq("abc", "abcdef @transbordeur abc", "abcdef@hotmail.fr")
    val expectedEventsName = Seq("abc", "abcdef", "abcdef@hotmail.fr")

    eventsName map  { refactorEventOrPlaceName } mustBe expectedEventsName
  }

  "render a string from a set of a websites" in {
    websiteSetToString(Set.empty) mustBe None
    websiteSetToString(Set("a")) mustBe Some("a")
    websiteSetToString(Set("a", "b", "c")) mustBe Some("a,b,c")
  }
}