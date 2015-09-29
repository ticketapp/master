package json

import java.util.UUID

import models.Accounting._
import models._
import play.api.libs.json.{JsNumber, _}

object JsonHelper {
  implicit object JavaBigDecimalWrites extends AnyRef with Writes[java.math.BigDecimal] {
    def writes(bigDecimal: java.math.BigDecimal): JsNumber = JsNumber(BigDecimal(bigDecimal))
  }

//  implicit object FloatWrites extends AnyRef with Writes[Float] {
//    def writes(float: Float): JsNumber = JsNumber(BigDecimal(float))
//  }

  implicit object CharWrites extends AnyRef with Writes[Char] {
    def writes(char: Char): JsString = JsString(char.toString)
  }

  implicit object UUIDWrites extends AnyRef with Writes[UUID] {
    def writes(UUID: UUID): JsString = JsString(UUID.toString)
  }

  implicit val account60Writes: Writes[Account60] = Json.writes[Account60]
  implicit val account63Writes: Writes[Account63] = Json.writes[Account63]
  implicit val account403Writes: Writes[Account403] = Json.writes[Account403]
  implicit val account413Writes: Writes[Account413] = Json.writes[Account413]
  implicit val account623Writes: Writes[Account623] = Json.writes[Account623]
  implicit val account626Writes: Writes[Account626] = Json.writes[Account626]
  implicit val account627Writes: Writes[Account627] = Json.writes[Account627]
  implicit val account708Writes: Writes[Account708] = Json.writes[Account708]
  implicit val account4686Writes: Writes[Account4686] = Json.writes[Account4686]
  implicit val genreWrites = Json.writes[Genre]
  implicit val tariffWrites: Writes[Tariff] = Json.writes[Tariff]
  implicit val imageWrites = Json.writes[Image]
  implicit val trackWrites: Writes[Track] = Json.writes[Track]
  implicit val playlistWrites = Json.writes[Playlist]
  implicit val artistWrites = Json.writes[Artist]
  implicit val addressWrites = Json.writes[Address]
  implicit val placeWrites = Json.writes[Place]
  implicit val organizerWrites = Json.writes[Organizer]
  implicit val organizerWithAddressWrites = Json.writes[OrganizerWithAddress]
  implicit val eventWrites = Json.writes[Event]
  implicit val infoWrites: Writes[Info] = Json.writes[Info]
  implicit val issueWrites: Writes[Issue] = Json.writes[Issue]
  implicit val mailWrites: Writes[Mail] = Json.writes[Mail]
  implicit val issueCommentWrites: Writes[IssueComment] = Json.writes[IssueComment]
}
