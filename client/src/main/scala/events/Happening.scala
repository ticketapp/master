package events

import java.util.Date

import addresses.Address
import artists.ArtistWithWeightedGenres
import genres.Genre
import organizers.OrganizerWithAddress
import places.PlaceWithAddress

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Happening(var id: Option[Long] = None,
                     var facebookId: Option[String] = None,
                     var isPublic: Boolean,
                     var isActive: Boolean,
                     var name: String,
                     var geographicPoint: String,
                     var description: Option[String] = None,
                     var startTime: Date,
                     var endTime: Option[Date] = None,
                     var ageRestriction: Int = 16,
                     var tariffRange: Option[String] = None,
                     var ticketSellers: Option[String] = None,
                     var imagePath: Option[String] = None)

//@JSExportAll
//case class Geometry(point: String)

@JSExportAll
case class MaybeSalableEvent(event: Happening, isSalable: Boolean)

@JSExportAll
case class HappeningWithRelations(event: Happening,
                                  organizers: js.Array[OrganizerWithAddress],
                                  artists: js.Array[ArtistWithWeightedGenres],
                                  places: js.Array[PlaceWithAddress],
                                  genres: js.Array[Genre],
                                  addresses: js.Array[Address])



