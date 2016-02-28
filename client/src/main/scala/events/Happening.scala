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
case class Happening(id: Option[Long] = None,
                     facebookId: Option[String] = None,
                     isPublic: Boolean,
                     isActive: Boolean,
                     name: String,
                     geographicPoint: String,
                     description: Option[String] = None,
                     startTime: Date,
                     endTime: Option[Date] = None,
                     ageRestriction: Int = 16,
                     tariffRange: Option[String] = None,
                     ticketSellers: Option[String] = None,
                     imagePath: Option[String] = None)

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



