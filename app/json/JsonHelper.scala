package json

import java.util.UUID

import addresses.Address
import artistsDomain.{Artist, ArtistWithWeightedGenres}
import com.vividsolutions.jts.io.{WKTReader, WKTWriter}
import eventsDomain.{Event, EventWithRelations}
import genresDomain.{Genre, GenreWithWeight}
import issues.{Issue, IssueComment}
import organizersDomain.{Organizer, OrganizerWithAddress}
import others.Tariff
import placesDomain.{Place, PlaceWithAddress}
import playlistsDomain._
import tracksDomain.{Track, TrackWithGenres}

import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json.{JsNumber, _}

object JsonHelper {
  implicit object JavaBigDecimalWrites extends AnyRef with Writes[java.math.BigDecimal] {
    def writes(bigDecimal: java.math.BigDecimal): JsNumber = JsNumber(BigDecimal(bigDecimal))
  }

  implicit object CharWrites extends AnyRef with Writes[Char] {
    def writes(char: Char): JsString = JsString(char.toString)
  }

  implicit object CharReads extends AnyRef with Reads[Char] {
    def reads(char: JsValue): JsResult[Char] = JsSuccess(char.toString()(0))
  }

  implicit object UUIDWrites extends AnyRef with Writes[UUID] {
    def writes(UUID: UUID): JsString = JsString(UUID.toString)
  }

  def geomJsonFormat[G <: Geometry]: Format[G] = Format[G](
    fjs = Reads.StringReads.map(fromWKT[G]),
    tjs = new Writes[G] {
      def writes(o: G): JsValue = JsString(toWKT(o))
    }
  )

  private val wktWriterHolder = new ThreadLocal[WKTWriter]
  private val wktReaderHolder = new ThreadLocal[WKTReader]

  private def toWKT(geom: Geometry): String = {
    if (wktWriterHolder.get == null) wktWriterHolder.set(new WKTWriter())
    wktWriterHolder.get.write(geom)
  }

  private def fromWKT[T](wkt: String): T = {
    if (wktReaderHolder.get == null) wktReaderHolder.set(new WKTReader())
    wktReaderHolder.get.read(wkt).asInstanceOf[T]
  }

  implicit val geometryJsonFormat = geomJsonFormat[Geometry]

//  implicit val account60Writes: Writes[Account60] = Json.writes[Account60]
//  implicit val account63Writes: Writes[Account63] = Json.writes[Account63]
//  implicit val account403Writes: Writes[Account403] = Json.writes[Account403]
//  implicit val account413Writes: Writes[Account413] = Json.writes[Account413]
//  implicit val account623Writes: Writes[Account623] = Json.writes[Account623]
//  implicit val account626Writes: Writes[Account626] = Json.writes[Account626]
//  implicit val account627Writes: Writes[Account627] = Json.writes[Account627]
//  implicit val account708Writes: Writes[Account708] = Json.writes[Account708]
//  implicit val account4686Writes: Writes[Account4686] = Json.writes[Account4686]

  implicit val genreWrites = Json.writes[Genre]
  implicit val tariffWrites: Writes[Tariff] = Json.writes[Tariff]
  implicit val trackWrites: Writes[Track] = Json.writes[Track]
  implicit val trackReads: Reads[Track] = Json.reads[Track]
  implicit val trackWithGenresWrites: Writes[TrackWithGenres] = Json.writes[TrackWithGenres]
  implicit val trackWithPlaylistRankWrites = Json.writes[TrackWithPlaylistRank]
  implicit val trackWithPlaylistRankAndGenres = Json.writes[TrackWithPlaylistRankAndGenres]
  implicit val playlistInfoWrites = Json.writes[Playlist]
  implicit val trackIdWithPlaylistRankWrites = Json.writes[TrackIdWithPlaylistRank]
  implicit val playlistWithTracksWrites = Json.writes[PlaylistWithTracks]
  implicit val playlistWithTracksWithGenres = Json.writes[PlaylistWithTracksWithGenres]
  implicit val playlistNameTracksIdAndRankWrites = Json.writes[PlaylistNameTracksIdAndRank]
  implicit val artistWrites = Json.writes[Artist]
  implicit val genreWithWeightWrites = Json.writes[GenreWithWeight]
  implicit val artistWithGenresWrites = Json.writes[ArtistWithWeightedGenres]
  implicit val addressWrites = Json.writes[Address]
  implicit val addressReads = Json.reads[Address]
  implicit val placeWrites = Json.writes[Place]
  implicit val placeReads = Json.reads[Place]
  implicit val placeWithAddressWrites = Json.writes[PlaceWithAddress]
  implicit val placeWithAddressReads = Json.reads[PlaceWithAddress]
  implicit val organizerWrites = Json.writes[Organizer]
  implicit val organizerReads = Json.reads[Organizer]
  implicit val organizerWithAddressWrites = Json.writes[OrganizerWithAddress]
  implicit val eventWrites = Json.writes[Event]
  implicit val eventReads = Json.reads[Event]
  implicit val eventWithRelationsWrites = Json.writes[EventWithRelations]
//  implicit val infoWrites: Writes[Info] = Json.writes[Info]
  implicit val issueWrites: Writes[Issue] = Json.writes[Issue]
  implicit val issueReads: Reads[Issue] = Json.reads[Issue]
//  implicit val mailWrites: Writes[Mail] = Json.writes[Mail]
  implicit val issueCommentWrites: Writes[IssueComment] = Json.writes[IssueComment]
  implicit val issueCommentReads: Reads[IssueComment] = Json.reads[IssueComment]
}
