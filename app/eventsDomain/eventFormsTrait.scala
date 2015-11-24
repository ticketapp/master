package eventsDomain

import addresses.geographicPointTrait
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._

trait eventFormsTrait extends geographicPointTrait {

  def eventFormApply(facebookId: Option[String], name: String, geographicPoint: Option[String], description: Option[String],
                startTime: DateTime, endTime: Option[DateTime], ageRestriction: Int, tariffRange: Option[String],
                ticketSellers: Option[String], imagePath: Option[String]/*, tariffs: List[Tariff], addresses: List[Address]*/)
  : Event =
    Event(None, facebookId, isPublic = true, isActive = true, name, optionStringToOptionPoint(geographicPoint), description, startTime,
      endTime, ageRestriction, tariffRange, ticketSellers, imagePath)//, List.empty, List.empty, tariffs, addresses)

  def eventFormUnapply(event: Event) = {
    Some((event.facebookId, event.name, Option(event.geographicPoint.getOrElse("").toString), event.description,
      event.startTime, event.endTime, event.ageRestriction, event.tariffRange, event.ticketSellers, event.imagePath/*,
      event.tariffs, event.addresses*/))
  }

  val eventBindingForm = Form(
    mapping(
      "facebookId"-> optional(nonEmptyText(3)),
      "name" -> nonEmptyText(2),
      "geographicPoint" -> optional(nonEmptyText(3)),
      "description" -> optional(nonEmptyText(2)),
      "startTime" -> jodaDate("yyyy-MM-dd HH:mm"),
      "endTime" -> optional(jodaDate("yyyy-MM-dd HH:mm")),
      "ageRestriction" -> number,
      "imagePath" -> optional(nonEmptyText(2)),
      "tariffRange" -> optional(nonEmptyText(3)),
      "ticketSellers" -> optional(nonEmptyText(3))//,
      //      "tariffs" -> list(
      //        mapping(
      //          "denomination" -> nonEmptyText,
      //          "nbTicketToSell" -> number,
      //          "price" -> bigDecimal,
      //          "startTime" -> date("yyyy-MM-dd HH:mm"),
      //          "endTime" -> date("yyyy-MM-dd HH:mm")
      //        )(Tariff.formApply)(Tariff.formUnapply)),
      //      "addresses" -> list(
      //        mapping(
      //          "city" -> optional(text(2)),
      //          "zip" -> optional(text(2)),
      //          "street" -> optional(text(2))
      //        )(Address.formApply)(Address.formUnapply))
    )(eventFormApply)(eventFormUnapply)
  )
}
