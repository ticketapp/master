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
                imagePath: Option[String], address: Option[Address]): PlaceWithAddress = {
    val point = optionStringToOptionPoint(geographicPoint)
    PlaceWithAddress(
      place = Place(
        id = None, 
        name = name, facebookId = facebookId, geographicPoint = point, description = description, websites = webSite, 
        capacity = capacity, 
        openingHours = openingHours, 
        imagePath = imagePath),
      address = address)
    
  }

  def placeFormUnapply(place: PlaceWithAddress) =
    Option((place.place.name, place.place.facebookId, Option(place.place.geographicPoint.toString),
      place.place.description, place.place.websites, place.place.capacity, place.place.openingHours,
      place.place.imagePath, Option(Address(
      id = None,
      geographicPoint = place.address.get.geographicPoint,
      city = place.address.get.city,
      zip = place.address.get.zip,
      street = place.address.get.street))))
}
