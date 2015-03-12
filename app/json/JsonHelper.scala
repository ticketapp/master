package json

//import controllers.Test2.{YoutubeTrack, FacebookArtist, SoundCloudTrack}
import models._
import securesocial.core.{OAuth2Info, OAuth1Info, PasswordInfo}
import play.api.libs.json.JsNumber
import play.api.libs.json._
import play.api.libs.functional.syntax._

object JsonHelper {
  implicit object JavaBigDecimalWrites extends AnyRef with Writes[java.math.BigDecimal] {
    def writes(o : java.math.BigDecimal): JsNumber = JsNumber(BigDecimal(o))
  }

  implicit val oAuth1InfoWrites = Json.writes[OAuth1Info]
  implicit val oAuth2InfoWrites = Json.writes[OAuth2Info]
  implicit val passwordInfoWrites = Json.writes[PasswordInfo]
  implicit val oAuth1InfoReads = Json.reads[OAuth1Info]
  implicit val oAuth2InfoReads = Json.reads[OAuth2Info]
  implicit val passwordInfoReads = Json.reads[PasswordInfo]
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
  implicit val artistWrites = Json.writes[Artist]
  implicit val addressWrites = Json.writes[Address]
  implicit val placeWrites = Json.writes[Place]
  implicit val organizerWrites = Json.writes[Organizer]
  implicit val eventWrites = Json.writes[Event]
  implicit val infoWrites: Writes[Info] = Json.writes[Info]
}
