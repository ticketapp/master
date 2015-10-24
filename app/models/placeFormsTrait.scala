package models

import play.api.data.Form
import play.api.data.Forms._

trait placeFormsTrait extends geographicPointTrait with addressFormsTrait {
  val placeBindingForm = Form(mapping(
    "name" -> nonEmptyText(2),
    "facebookId" -> optional(nonEmptyText()),
    "geographicPoint" -> optional(nonEmptyText(5)),
    "description" -> optional(nonEmptyText(2)),
    "webSite" -> optional(nonEmptyText(4)),
    "capacity" -> optional(number),
    "openingHours" -> optional(nonEmptyText(4)),
    "imagePath" -> optional(nonEmptyText(2)),
    "address" -> addressBindingForm
  )(placeFormApply)(placeFormUnapply))

  def placeFormApply(name: String, facebookId: Option[String], geographicPoint: Option[String], description: Option[String],
                webSite: Option[String], capacity: Option[Int], openingHours: Option[String],
                imagePath: Option[String], address: Option[Address]): Place = {
    val point = optionStringToOptionPoint(geographicPoint)
    Place(None, name, facebookId, point, description, webSite, capacity, openingHours, imagePath/*, address*/)
  }

  def placeFormUnapply(place: Place) =
    Option((place.name, place.facebookId, Option(place.geographicPoint.toString), place.description, place.webSites,
      place.capacity, place.openingHours, place.imagePath, None/*, place.address.get.city, place.address.get.zip, place.address.get.street*/))
}
