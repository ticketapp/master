package places

import addresses.Address

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Place(id: Option[Long] = None,
                 name: String,
                 facebookId: Option[String] = None,
                 geographicPoint: String,
                 description: Option[String] = None,
                 websites: Option[String] = None,
                 capacity: Option[Int] = None,
                 openingHours: Option[String] = None,
                 imagePath: Option[String] = None,
                 addressId: Option[Long] = None,
                 linkedOrganizerId: Option[Long] = None)

@JSExportAll
case class PlaceWithAddress(place: Place, maybeAddress: Option[Address] =  None)
