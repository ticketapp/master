import org.scalatestplus.play._

class TestTariffModel extends PlaySpec with OneAppPerSuite with Injectors {

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

    "find ticket seller" in {
      val fnacTicket = tariffMethods.findTicketSellers(Set("lasasconcerts.fnacspectacles.com/place-spectacle/manifestation/musique-electronique-microphone-recordings-party-86273.htm"))
      val digitick = tariffMethods.findTicketSellers(Set("digitick.com"))
      fnacTicket mustBe Some("lasasconcerts.fnacspectacles.com/place-spectacle/manifestation/musique-electronique-microphone-recordings-party-86273.htm")
      digitick mustBe empty
    }
  }
}
