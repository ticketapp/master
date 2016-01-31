package events

import addresses.Address
import artists.ArtistWithWeightedGenres
import genres.Genre
import organizers.OrganizerWithAddress
import places.PlaceWithAddress

import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSExportAll
@JSExportAll
case class Happening(id: Option[Long] ,
                  facebookId: Option[String] ,
                  isPublic: Boolean,
                  isActive: Boolean,
                  name: String,
                  geographicPoint: Geometry,
                  description: Option[String] ,
                  startTime: Date,
                  endTime: Option[Date] ,
                  ageRestriction: Int = 16,
                  tariffRange: Option[String] ,
                  ticketSellers: Option[String] ,
                  imagePath: Option[String] )

case class Geometry(point: String)


@JSExportAll
case class EventWithRelations(event: Happening,
                              organizers: Seq[OrganizerWithAddress],
                              artists: Seq[ArtistWithWeightedGenres],
                              places: Seq[PlaceWithAddress],
                              genres: Seq[Genre],
                              addresses: Seq[Address])



