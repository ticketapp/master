import org.scalatest.concurrent.ScalaFutures._
import tariffsDomain.Tariff
import testsHelper.GlobalApplicationForModels
import org.joda.time.DateTime


class TestTariffModel extends GlobalApplicationForModels {

  val savedTariff = Tariff(
    tariffId = Some(10000),
    denomination = "test",
    eventId = 100,
    startTime = new DateTime("2040-08-24T14:00:00.000+02:00"),
    endTime = new DateTime("2040-09-24T14:00:00.000+02:00"),
    price = 10.0)

  "Tariff" must {

    "find prices" in {
      tariffMethods.findPricesInDescription(None) mustBe None
      tariffMethods.findPricesInDescription(Some("ion no no 6€ jlk ljk klj klj 7€")) mustBe Some("6.0-7.0")
      tariffMethods.findPricesInDescription(Some("ion no no 7* / 9 €  jlk ljk klj klj")) mustBe Some("9.0-9.0")
      tariffMethods.findPricesInDescription(Some("ion no no 6€ jlk ljk klj klj 145€ dsq q dqsdqsd q 4€")) mustBe Some("4.0-145.0")
      tariffMethods.findPricesInDescription(Some("ion no no 6€ jlk ljk klj klj 7€ dsq q dqsdqsd q 4€/8€ qsdqsd")) mustBe Some("4.0-8.0")
      tariffMethods.findPricesInDescription(Some("ion no no 6€ jlk ljk klj klj 3  € dsq q dqsdqsd q 4€/8 € qsdqsd")) mustBe Some("3.0-8.0")
      tariffMethods.findPricesInDescription(Some("ion no no 6.8€ jlk ljk klj klj 3,45 € dsq q 16 dqsdqsd q 4€/5 € qsdqsd")) mustBe Some("3.45-6.8")
      tariffMethods.findPricesInDescription(Some("ion no no 6.8 € jlk ljk klj klj 3.11 € dsq q dqsdqsd q 4€/5 € qsdqsd")) mustBe Some("3.11-6.8")
      tariffMethods.findPricesInDescription(Some("ion no no 15€/10€/7.5€ jlk ljk klj klj dsq q dqsdqsd q  qsdqsd")) mustBe Some("7.5-15.0")
    }

    "find ticket seller" in {
      val fnacTicket = tariffMethods.findTicketSellers(Set("lasasconcerts.fnacspectacles.com/place-spectacle/manifestation/musique-electronique-microphone-recordings-party-86273.htm"))
      val digitick = tariffMethods.findTicketSellers(Set("digitick.com"))
      fnacTicket mustBe Some("lasasconcerts.fnacspectacles.com/place-spectacle/manifestation/musique-electronique-microphone-recordings-party-86273.htm")
      digitick mustBe empty
    }

    "return its id when saved" in {
      val tariff = Tariff(
        denomination = "test",
        eventId = 1,
        startTime = new DateTime(),
        endTime = new DateTime(),
        price = 10)

      whenReady(tariffMethods.save(tariff)) { tariffId => tariffId mustBe 1 }
    }

    "be found by eventId" in {
      whenReady(tariffMethods.findByEventId(savedTariff.eventId)) { tariffs => tariffs must contain(savedTariff) }
    }
  }
}
