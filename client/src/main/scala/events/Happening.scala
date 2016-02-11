package events

import addresses.Address
import artists.ArtistWithWeightedGenres
import genres.Genre
import organizers.OrganizerWithAddress
import places.PlaceWithAddress

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSExportAll


@JSExportAll
case class Happening(var id: Option[Long],
                     var facebookId: Option[String] ,
                     var isPublic: Boolean,
                     var isActive: Boolean,
                     var name: String,
                     var geographicPoint: Geometry,
                     var description: Option[String] ,
                     var startTime: Date,
                     var endTime: Option[Date] ,
                     var ageRestriction: Int = 16,
                     var tariffRange: Option[String] ,
                     var ticketSellers: Option[String] ,
                     var imagePath: Option[String] )

@JSExportAll
case class Geometry(point: String)


@JSExportAll
case class HappeningWithRelations(event: Happening,
                              organizers: js.Array[OrganizerWithAddress],
                              artists: js.Array[ArtistWithWeightedGenres],
                              places: js.Array[PlaceWithAddress],
                              genres: js.Array[Genre],
                              addresses: js.Array[Address])



