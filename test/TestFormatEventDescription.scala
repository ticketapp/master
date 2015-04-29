import org.scalatestplus.play._
import org.scalatest._
import services.Utilities.formatDescription

class TestFormatEventDescription extends PlaySpec with OneAppPerSuite {

  "Format a description" must {

    "should not format mail addresses as a link" in {
      val description = Option("roman.trystram@caa.com")

      val formattedDescription = formatDescription(description)
      println(formattedDescription)

      val expectedResult = Option("<div class='column large-12'><i>roman.trystram@caa.com</i></div>")

      formattedDescription mustBe expectedResult
    }

    "should not format phone numbers as a link" in {
      val description = Option("06.60.63.14.16")

      val formattedDescription = formatDescription(description)
      println(formattedDescription)

      val expectedResult = Option("<div class='column large-12'>06.60.63.14.16</div>")

      formattedDescription mustBe expectedResult
    }
  }
}
/*
Some("<div class='column large-12'>roman.trystram@<a href='http://caa.com'>caa.com</a></div>") was not equal to Some("<i>roman.trystram@caa.com</i>")
 */