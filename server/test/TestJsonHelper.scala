import play.api.libs.json.{JsString, Json, Writes}
import testsHelper.GlobalApplicationForModels


class TestJsonHelper extends GlobalApplicationForModels {

  case class A(a: Option[String])

  "Json helper" must {

    "write a case class with option in a custom way" in {

      implicit val aWrites: Writes[A] = Json.writes[A]

      implicit def ow[T](implicit w: Writes[T]): Writes[Option[T]] = Writes {
        case None => JsString("[]")
        case Some(t) => Json.toJson(t)
      }

      Json.toJson(A(a = None)) mustBe Json.parse("""{"a":"[]"}""")
    }
  }
}
