import java.util.Date
import controllers.DAOException
import models.Artist
import models.Artist._
import org.postgresql.util.PSQLException
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
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

class TestArtistController extends PlaySpec with OneAppPerSuite {

  "ArtistController" must {

    "be able to be followed by a user" in new WithApplication {
      val result = controllers.Application.index()(FakeRequest())
      println(result)
      result map { a => a mustBe "Ok" }
    }
  }
}
