import services.Utilities._
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.{JsValue, Json}
import models.Artist._

class TestUtilities extends PlaySpec with OneAppPerSuite {

  "A utilities" must {

    "return a normalized string with the method normalizeString" in {
      val strings = List("éh'=)àç_è-(aék", "abc de#f")

      val normalizedString: List[String] = strings.map { normalizeString }

      val expectedResult = List("éh'=)àç_è-(aék", "abc de#f")

      normalizedString mustBe expectedResult
    }
  }//List("éh'=)àç_è-(aék", "abc de
  // List("éh'=)àç_è-(aék", "abc de#f") (TestUtilities.scala:20)
}

