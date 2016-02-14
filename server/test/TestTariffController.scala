import database.MyPostgresDriver.api._
import json.JsonHelper
import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsResult, JsSuccess}
import play.api.test.FakeRequest
import tariffsDomain.Tariff
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.duration._


class TestTariffController extends GlobalApplicationForControllers {

  override def beforeAll() {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(
        sqlu"""INSERT INTO events(eventid, ispublic, isactive, name, starttime, geographicpoint)
          VALUES(100, true, true, 'notPassedEvent2', timestamp '2050-08-24 14:00:00',
          '01010000008906CEBE97E346405187156EF9581340');"""),
      2.seconds)
    Await.result(
      dbConfProvider.get.db.run(
        sqlu"""INSERT INTO tariffs(tariffId, denomination, price, startTime, endTime, eventId)
          VALUES(10000, 'test', 10, TIMESTAMP WITH TIME ZONE '2040-08-24T14:00:00.000+02:00',
          TIMESTAMP WITH TIME ZONE '2040-09-24T14:00:00.000+02:00', 100);"""),
      2.seconds)
  }

  "TariffController" should {

    val savedTariff = Tariff(
      tariffId = Some(10000),
      denomination = "test",
      eventId = 100,
      startTime = new DateTime("2040-08-24T14:00:00.000+02:00"),
      endTime = new DateTime("2040-09-24T14:00:00.000+02:00"),
      price = 10.0)

    "get tariffs by event id" in {
      val Some(info) = route(FakeRequest(tariffsDomain.routes.TariffController.findTariffsByEventId(100)))
      val validatedJsonSalableEvents: JsResult[Seq[Tariff]] =
        contentAsJson(info).validate[Seq[Tariff]](JsonHelper.readTariffReads)

      val expectedEvents = validatedJsonSalableEvents match {
        case error: JsError => throw new Exception("get tariffs by event id")
        case events: JsSuccess[Seq[Tariff]] => events.get
      }

      expectedEvents must contain(savedTariff)
    }

    "save a new tariffs" in {
      val Some(info) = route(FakeRequest(tariffsDomain.routes.TariffController.save(
        "saveTest", savedTariff.eventId, "2040-08-24T14:00:00.000+02:00", "2040-09-24T14:00:00.000+02:00", 20.0
      )))

      contentAsString(info).toInt mustEqual 1
    }
  }
}