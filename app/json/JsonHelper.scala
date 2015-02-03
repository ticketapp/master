package json

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

  /*implicit val oAuth1InfoWrites = new Writes[OAuth1Info] {
    def writes(oAuth1Info: OAuth1Info) = Json.obj(
      "token" -> JsString(oAuth1Info.token),
      "secret" -> JsString(oAuth1Info.secret) )
  }
  = new Writes[OAuth2Info] {
    def writes(oAuth2Info: OAuth2Info) = Json.obj(
      "accessToken" -> JsString(oAuth2Info.accessToken),
      "tokenType" -> Json.toJson(oAuth2Info.tokenType),
      "expiresIn" -> Json.toJson(oAuth2Info.expiresIn),
      "refreshToken" -> Json.toJson(oAuth2Info.refreshToken) )
  }
  implicit val passwordInfoWrites = new Writes[PasswordInfo] {
    def writes(passwordInfo: PasswordInfo) = Json.obj(
      "hasher" -> JsString(passwordInfo.hasher),
      "password" -> JsString(passwordInfo.password),
      "salt" -> Json.toJson(passwordInfo.salt) )
  }*/
  /*implicit val oAuth1InfoFormat = (
    (__ \ "token").format[String] and
      (__ \ "secret").format[String]
    )(OAuth1Info.apply, unlift(OAuth1Info.unapply))*/


  implicit val account60Writes: Writes[Account60] = Json.writes[Account60]
  implicit val account63Writes: Writes[Account63] = Json.writes[Account63]
  implicit val account403Writes: Writes[Account403] = Json.writes[Account403]
  implicit val account413Writes: Writes[Account413] = Json.writes[Account413]
  implicit val account623Writes: Writes[Account623] = Json.writes[Account623]
  implicit val account626Writes: Writes[Account626] = Json.writes[Account626]
  implicit val account627Writes: Writes[Account627] = Json.writes[Account627]
  implicit val account708Writes: Writes[Account708] = Json.writes[Account708]
  implicit val account4686Writes: Writes[Account4686] = Json.writes[Account4686]
  implicit val tariffWrites: Writes[Tariff] = Json.writes[Tariff]
  implicit val eventWrites = Json.writes[Event]
}
