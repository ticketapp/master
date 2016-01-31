package places

import addresses.Address
import events.Geometry

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Place(id: Option[Long],
                 name: String,
                 facebookId: Option[String],
                 geographicPoint: Geometry,
                 description: Option[String],
                 websites: Option[String],
                 capacity: Option[Int],
                 openingHours: Option[String],
                 imagePath: Option[String],
                 addressId: Option[Long],
                 linkedOrganizerId: Option[Long])

@JSExportAll
case class PlaceWithAddress(place: Place, maybeAddress: Option[Address])
