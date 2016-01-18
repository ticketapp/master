import database.MyDBTableDefinitions
import testsHelper.GlobalApplicationForModels


class TestMyDBTableDefinition extends GlobalApplicationForModels with MyDBTableDefinitions{

  "MDBT" must {

    "take an option of string separated with comma and return to set" in {
      val maybeString = Option("a, b, c, ")
      optionStringToSet(maybeString) mustBe Set("a", "b", "c")
    }
  }
}
