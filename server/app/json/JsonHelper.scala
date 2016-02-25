package json

import java.sql.Timestamp

import addresses.Address
import artistsDomain.{Artist, ArtistWithWeightedGenres}
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.{WKTReader, WKTWriter}
import eventsDomain.{Event, EventWithRelations}
import genresDomain.{Genre, GenreWithWeight}
import issues.{Issue, IssueComment}
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.{Place, PlaceWithAddress}
import play.api.libs.json.{JsNumber, _}
import playlistsDomain._
import tariffsDomain.Tariff
import ticketsDomain._
import trackingDomain.{UserAction, UserSession}
import tracksDomain.{Track, TrackWithGenres}
import userDomain.{IdCard, Rib, FromClientRib}

object JsonHelper {

  implicit object JavaBigDecimalWrites extends AnyRef with Writes[java.math.BigDecimal] {
    def writes(bigDecimal: java.math.BigDecimal): JsNumber = JsNumber(BigDecimal(bigDecimal))
  }

  implicit object CharWrites extends AnyRef with Writes[Char] {
    def writes(char: Char): JsString = JsString(char.toString)
  }

  implicit object CharReads extends AnyRef with Reads[Char] {
    def reads(char: JsValue): JsResult[Char] = JsSuccess(Json.stringify(char)(1))
  }

  implicit object TimestampReads extends AnyRef with Reads[Timestamp] {
    def reads(char: JsValue): JsResult[Timestamp] = JsSuccess(new Timestamp(Json.stringify(char).toLong))
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

  //  implicit val account60Format: Format[Account60] = Json.format[Account60]
  //  implicit val account63Format: Format[Account63] = Json.format[Account63]
  //  implicit val account403Format: Format[Account403] = Json.format[Account403]
  //  implicit val account413Format: Format[Account413] = Json.format[Account413]
  //  implicit val account623Format: Format[Account623] = Json.format[Account623]
  //  implicit val account626Format: Format[Account626] = Json.format[Account626]
  //  implicit val account627Format: Format[Account627] = Json.format[Account627]
  //  implicit val account708Format: Format[Account708] = Json.format[Account708]
  //  implicit val account4686Format: Format[Account4686] = Json.format[Account4686]

  implicit val genreFormat = Json.format[Genre]
  implicit val fromClientRibFormat = Json.format[FromClientRib]
  implicit val ribFormat = Json.format[Rib]
  implicit val idCardFormat = Json.format[IdCard]
  implicit val trackFormat: Format[Track] = Json.format[Track]
  implicit val trackWithGenresFormat: Format[TrackWithGenres] = Json.format[TrackWithGenres]
  implicit val trackWithPlaylistRankFormat = Json.format[TrackWithPlaylistRank]
  implicit val trackWithPlaylistRankAndGenres = Json.format[TrackWithPlaylistRankAndGenres]
  implicit val playlistInfoFormat = Json.format[Playlist]
  implicit val trackIdWithPlaylistRankFormat = Json.format[TrackIdWithPlaylistRank]
  implicit val playlistWithTracksFormat = Json.format[PlaylistWithTracks]
  implicit val playlistWithTracksWithGenres = Json.format[PlaylistWithTracksWithGenres]
  implicit val playlistNameTracksIdAndRankFormat = Json.format[PlaylistNameTracksIdAndRank]
  implicit val artistFormat: Format[Artist] = Json.format[Artist]
  implicit val genreWithWeightFormat = Json.format[GenreWithWeight]
  implicit val artistWithGenresFormat = Json.format[ArtistWithWeightedGenres]
  implicit val addressFormat = Json.format[Address]
  implicit val placeFormat = Json.format[Place]
  implicit val placeWithAddressFormat = Json.format[PlaceWithAddress]
  implicit val organizerFormat = Json.format[Organizer]
  implicit val organizerWithAddressFormat = Json.format[OrganizerWithAddress]
  implicit val eventFormat = Json.format[Event]
  implicit val eventWithRelationsFormat = Json.format[EventWithRelations]
  implicit val issueFormat: Format[Issue] = Json.format[Issue]
  implicit val issueCommentFormat: Format[IssueComment] = Json.format[IssueComment]
  implicit val ticketFormat: Format[Ticket] = Json.format[Ticket]
  implicit val statusFormat: Format[TicketStatus] = Json.format[TicketStatus]
  implicit val ticketWithStatusFormat: Format[TicketWithStatus] = Json.format[TicketWithStatus]
  implicit val salableEventFormat: Format[SalableEvent] = Json.format[SalableEvent]
  implicit val pendingTicketFormat: Format[PendingTicket] = Json.format[PendingTicket]
  implicit val ticketBillFormat: Format[TicketBill] = Json.format[TicketBill]
  implicit val tariffFormat: Format[Tariff] = Json.format[Tariff]
  implicit val userActionFormat: Format[UserAction] = Json.format[UserAction]
  implicit val userSessionFormat: Format[UserSession] = Json.format[UserSession]
  implicit val maybeSalableEventFormat: Format[MaybeSalableEvent] = Json.format[MaybeSalableEvent]

  val readUserActionReads: Reads[Seq[UserAction]] = Reads.seq(__.read[UserAction])
  val readUserSessionReads: Reads[Seq[UserSession]] = Reads.seq(__.read[UserSession])
  val readSalableEventReads: Reads[Seq[SalableEvent]] = Reads.seq(__.read[SalableEvent])
  val readTicketWithStatusReads: Reads[Seq[TicketWithStatus]] = Reads.seq(__.read[TicketWithStatus])
  val readPendingTicketReads: Reads[Seq[PendingTicket]] = Reads.seq(__.read[PendingTicket])
  val readTicketBillReads: Reads[Seq[TicketBill]] = Reads.seq(__.read[TicketBill])
  val readMaybeSalableEventReads: Reads[Seq[MaybeSalableEvent]] = Reads.seq(__.read[MaybeSalableEvent])
  val readTariffReads: Reads[Seq[Tariff]] = Reads.seq(__.read[Tariff])
  val readRibReads: Reads[Seq[Rib]] = Reads.seq(__.read[Rib])
  val readIdCardReads: Reads[Seq[IdCard]] = Reads.seq(__.read[IdCard])
}
