import org.scalatestplus.play._
import org.scalatest._
import services.Utilities.formatDescription

class TestFormatEventDescription extends PlaySpec {

  "A formatted description" must {

    "should not have formatted mail as a url" in {
      val description = Option("roman.trystram@caa.com")

      val formattedDescription = formatDescription(description)

      val expectedResult = "<i>roman.trystram@caa.com</i>"

      formattedDescription mustBe expectedResult
    }
  }
}
