import json.JsonHelper
import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsResult, JsSuccess}
import play.api.test.FakeRequest
import tariffsDomain.Tariff
import testsHelper.GlobalApplicationForControllers

class TestTariffController extends GlobalApplicationForControllers {
  sequential

  val savedTariff = Tariff(
    tariffId = Some(10000),
    denomination = "test",
    eventId = 100,
    startTime = new DateTime("2040-08-24T14:00:00.000+02:00"),
    endTime = new DateTime("2040-09-24T14:00:00.000+02:00"),
    price = 10.0)

  "TariffController" should {

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