import play.api.libs.json.Json
import testsHelper._
import json.JsonHelper._


class TestJsonHelper extends GlobalApplicationForModels {

  "Json helper" must {

    "write a char as a string" in {

      Json.toJson('a') mustBe Json.parse(""""a"""")
    }
  }
}
