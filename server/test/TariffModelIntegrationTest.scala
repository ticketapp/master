import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures._
import tariffsDomain.Tariff
import testsHelper.GlobalApplicationForModelsIntegration
import database.MyPostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class TariffModelIntegrationTest extends GlobalApplicationForModelsIntegration {

  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO events(eventid, ispublic, isactive, name, starttime, geographicpoint)
           VALUES(1, true, true, 'notPassedEvent1', TIMESTAMP WITH TIME ZONE '2040-08-24 14:00:00',
          '01010000008906CEBE97E345405187156EF9581340');
        INSERT INTO events(eventid, ispublic, isactive, name, starttime, geographicpoint)
           VALUES(100, true, true, 'notPassedEvent2', TIMESTAMP WITH TIME ZONE '2050-08-24 14:00:00',
          '01010000008906CEBE97E346405187156EF9581340');
        INSERT INTO tariffs(tariffId, denomination, price, startTime, endTime, eventId)
          VALUES(10000, 'test', 10, TIMESTAMP WITH TIME ZONE '2040-08-24T14:00:00.000+02:00',
          TIMESTAMP WITH TIME ZONE '2040-09-24T14:00:00.000+02:00', 100);"""),
      2.seconds)
  }

  val savedTariff = Tariff(
    tariffId = Some(10000),
    denomination = "test",
    eventId = 100,
    startTime = new DateTime("2040-08-24T14:00:00.000+02:00"),
    endTime = new DateTime("2040-09-24T14:00:00.000+02:00"),
    price = 10.0)

  "Tariff" must {

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
