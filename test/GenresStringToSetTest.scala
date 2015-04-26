import scala.collection.mutable
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import models.Genre.genresStringToGenresSet

class GenresStringToSetTest extends PlaySpec {

  "A string of genres" must {

    "be well normalized into a set of genres" in {

      val genres =  1

//      stack.pop() mustBe 2
//      stack.pop() mustBe 1
    }
  }
}
