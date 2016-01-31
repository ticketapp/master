package organizers


import addresses.Address
import events.Geometry

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Organizer(id: Option[Long],
                      facebookId: Option[String],
                      name: String,
                      description: Option[String],
                      addressId: Option[Long],
                      phone: Option[String],
                      publicTransit: Option[String],
                      websites: Option[String],
                      verified: Boolean,
                      imagePath: Option[String],
                      geographicPoint: Geometry,
                      linkedPlaceId: Option[Long])

@JSExportAll
case class OrganizerWithAddress(organizer: Organizer, address: Option[Address])
