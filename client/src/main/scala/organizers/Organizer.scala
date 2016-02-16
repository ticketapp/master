package organizers

import addresses.Address

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Organizer(id: Option[Long] = None,
                     facebookId: Option[String] = None,
                     name: String,
                     description: Option[String] = None,
                     addressId: Option[Long] = None,
                     phone: Option[String] = None,
                     publicTransit: Option[String] = None,
                     websites: Option[String] = None,
                     verified: Boolean,
                     imagePath: Option[String] = None,
                     geographicPoint: String,
                     linkedPlaceId: Option[Long] = None)

@JSExportAll
case class OrganizerWithAddress(organizer: Organizer, address: Option[Address] = None)
