import models.TariffMethods
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.Utilities

class TestTariffModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities
  val tariffMethods = new TariffMethods(dbConfProvider, utilities)

  "Tariff" must {

    "find prices" in {
      tariffMethods.findPrices(None) mustBe None
      tariffMethods.findPrices(Some("ion no no 6€ jlk ljk klj klj 7€")) mustBe Some("6.0-7.0")
      tariffMethods.findPrices(Some("ion no no 7* / 9 €  jlk ljk klj klj")) mustBe Some("9.0-9.0")
      tariffMethods.findPrices(Some("ion no no 6€ jlk ljk klj klj 145€ dsq q dqsdqsd q 4€")) mustBe Some("4.0-145.0")
      tariffMethods.findPrices(Some("ion no no 6€ jlk ljk klj klj 7€ dsq q dqsdqsd q 4€/8€ qsdqsd")) mustBe Some("4.0-8.0")
      tariffMethods.findPrices(Some("ion no no 6€ jlk ljk klj klj 3  € dsq q dqsdqsd q 4€/8 € qsdqsd")) mustBe Some("3.0-8.0")
      tariffMethods.findPrices(Some("ion no no 6.8€ jlk ljk klj klj 3,45 € dsq q dqsdqsd q 4€/5 € qsdqsd")) mustBe Some("3.45-6.8")
      tariffMethods.findPrices(Some("ion no no 6.8 € jlk ljk klj klj 3.11 € dsq q dqsdqsd q 4€/5 € qsdqsd")) mustBe Some("3.11-6.8")
    }
  }
}
