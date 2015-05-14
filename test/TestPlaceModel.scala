import java.util.Date
import controllers.DAOException
import models.{Address, Place}
import models.Place._
import org.postgresql.util.PSQLException
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.util.Success
import scala.util.Failure
import play.api.libs.concurrent.Execution.Implicits._

class TestPlaceModel extends PlaySpec with OneAppPerSuite {

  "A place" must {

//    val place = Place(None, "name", Option("facebookId1"), Option("(0,0)"))
    val place = Place(None, "Le Transbordeur", Some("164354640267171"), None,
      Some("""<div class="column large-12">Ancienne usine destinée à l’origine au traitement des eaux, le bâtiment situé sur la commune de Villeen fait un lieu de référence au niveau régional, national, voire international !</div>"""),
      Some("www.transbordeur.fr"), Some(9099), None,
      Some("https://scontent.xx.fbcdn.net/hphotos-xfa1/t31.0-8/s720x720/10699809_743219622380667_4218063112874186795_o.jpg"),
      Some(Address(None,None,None,None,None)))


    "be able to be saved and deleted in database and return the new id" in {
      save(place).map { placeId =>
        find(placeId.get.get) mustEqual Option(place.copy(placeId = placeId.get))
        delete(placeId.get.get) mustBe Success(Option(1))
      }
    }

    "be able to be followed and unfollowed by a user" in {
      followByPlaceId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByPlaceId("userTestId", 1) mustBe 1
    }

    "not be able to be followed twice" in {
      followByPlaceId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      followByPlaceId("userTestId", 1) shouldBe a [Failure[PSQLException]]
      unfollowByPlaceId("userTestId", 1) mustBe 1
    }
  }
}
