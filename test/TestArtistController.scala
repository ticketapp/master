import java.util.Date
import controllers.DAOException
import models.Artist
import models.Artist._
import models.Event
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.Json
import play.api.test.{WithApplication, PlaySpecification, FakeRequest}
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.util.Success
import scala.util.Failure
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test._
import play.api.test.Helpers._
import json.JsonHelper._

class TestArtistController extends PlaySpec with OneAppPerSuite {

  "ArtistController" must {

    "be able to be followed by a user" in {
      val eventuallyResult = controllers.EventController.findNearCity("abc", 1, 0)(FakeRequest())
      status(eventuallyResult) mustBe 200
      println(contentAsJson(eventuallyResult))
      contentAsJson(eventuallyResult) mustBe Json.toJson(Seq.empty[Event])
    }
  }
}
