import java.util.Date
import controllers.DAOException
import models.{Track, Artist, Tariff}
import models.Tariff._
import org.postgresql.util.PSQLException
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import securesocial.core.Identity

import play.api.Play.current

import scala.util.Success
import scala.util.Failure
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import java.util.UUID.randomUUID

class TestTariffModel extends PlaySpec with OneAppPerSuite {

  "Tariff" must {

    "find prices" in {
      findPrices(None) mustBe None
      findPrices(Some("ion no no 6€ jlk ljk klj klj 7€")) mustBe Some("6.0-7.0")
      findPrices(Some("ion no no 7* / 9 €  jlk ljk klj klj")) mustBe Some("9.0-9.0")
      findPrices(Some("ion no no 6€ jlk ljk klj klj 145€ dsq q dqsdqsd q 4€")) mustBe Some("4.0-145.0")
      findPrices(Some("ion no no 6€ jlk ljk klj klj 7€ dsq q dqsdqsd q 4€/8€ qsdqsd")) mustBe Some("4.0-8.0")
      findPrices(Some("ion no no 6€ jlk ljk klj klj 3  € dsq q dqsdqsd q 4€/8 € qsdqsd")) mustBe Some("3.0-8.0")
      findPrices(Some("ion no no 6.8€ jlk ljk klj klj 3,45 € dsq q dqsdqsd q 4€/5 € qsdqsd")) mustBe Some("3.45-6.8")
      findPrices(Some("ion no no 6.8 € jlk ljk klj klj 3.11 € dsq q dqsdqsd q 4€/5 € qsdqsd")) mustBe Some("3.11-6.8")
    }
  }
}
