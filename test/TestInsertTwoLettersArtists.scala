import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models._
import models.Playlist._
import models.Track
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import java.util.UUID.randomUUID

import scala.util.Success

class TestInsertTwoLettersArtists extends PlaySpec with OneAppPerSuite {

  "Insert two letters artists" must {

    "list all combination of two chars" in {
      val chars = "abcdefghijklmnopqrstuvwxyzàçéèëêîïôöùûü0123456789?!$+".toArray

      for (c1 <- chars) {
        for (c2 <- chars) {
          println(c1.toString + c2.toString)
        }
      }
    }
  }
}
