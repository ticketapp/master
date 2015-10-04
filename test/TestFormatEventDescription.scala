import org.scalatestplus.play._
import org.scalatest._
import services.Utilities.formatDescription

class TestFormatEventDescription extends PlaySpec with OneAppPerSuite {

  "Format a description" must {

    "should format links" in {
      val descriptions = List(Option("Hello, I'm a www.link.com, au revoir."), Option("Hello, I'm a link.fr, au revoir."),
        Option("Hello, I'm a http://www.link.com, au revoir."), Option("Hello, I'm a https://www.link.com, au revoir."),
        Option("Hello, I'm a http://link.org, au revoir."), Option("Hello, I'm a https://link.org"),
        Option(
          """Hello, I'm a https://link.org
            |
            |go to the line
          """.stripMargin))

      val formattedDescriptions = descriptions.map { formatDescription }

      val expectedResult = List(
        Option("<div class='column large-12'>Hello, I'm a <a href='http://link.com'>link.com</a>, au revoir.</div>"),
        Option("<div class='column large-12'>Hello, I'm a <a href='http://link.fr'>link.fr</a>, au revoir.</div>"),
        Option("<div class='column large-12'>Hello, I'm a <a href='http://link.com'>link.com</a>, au revoir.</div>"),
        Option("<div class='column large-12'>Hello, I'm a <a href='http://link.com'>link.com</a>, au revoir.</div>"),
        Option("<div class='column large-12'>Hello, I'm a <a href='http://link.org'>link.org</a>, au revoir.</div>"),
        Option("<div class='column large-12'>Hello, I'm a <a href='http://link.org'>link.org</a></div>"),
        Option("<div class='column large-12'>Hello, I'm a <a href='http://link.org'>link.org</a><br/><br/></div>" +
          "<div class='column large-12'>go to the line<br/></div>"))

      formattedDescriptions mustBe expectedResult
    }

    "should not format mail addresses as a link" in {
      val description = Option("roman.trystram@caa.com")

      val formattedDescription = formatDescription(description)

      val expectedResult = Option("<div class='column large-12'>roman.trystram@caa.com</div>")

      formattedDescription mustBe expectedResult
    }

    "should not format phone numbers as a link" in {
      val description = Option("06.60.63.14.16")

      val formattedDescription = formatDescription(description)

      val expectedResult = Option("<div class='column large-12'>06.60.63.14.16</div>")

      formattedDescription mustBe expectedResult
    }
  }
}
