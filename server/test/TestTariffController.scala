import json.JsonHelper
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsResult}
import play.api.test.FakeRequest
import tariffsDomain.Tariff
import org.joda.time.DateTime
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
      val validatedJsonSellableEvents: JsResult[Seq[Tariff]] =
        contentAsJson(info).validate[Seq[Tariff]](JsonHelper.readTariffReads)
      validatedJsonSellableEvents match {
        case events: JsSuccess[Seq[Tariff]] =>
          events.get must contain (savedTariff)
        case error: JsError =>
          Logger.error("get all sallable events:" + error)
          error mustEqual 0
      }
    }
    "save a new tariffs" in {
      val Some(info) = route(FakeRequest(tariffsDomain.routes.TariffController.save(
        "saveTest", 100, "2040-08-24T14:00:00.000+02:00", "2040-09-24T14:00:00.000+02:00",20.0
      )))

      contentAsString(info).toInt mustEqual 1

    }
  }
}