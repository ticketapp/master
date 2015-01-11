package json

import models._
import play.api.libs.json._
import play.api.libs.json.JsString
import play.api.libs.functional.syntax
import scala.collection.Traversable
import scala.Traversable
import play.api.libs.json.Json._
import play.api.libs.json.JsArray
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsNumber
import play.api.libs.json.{JsString, _}
import play.api.libs.json.DefaultWrites
import scala.BigDecimal.javaBigDecimal2bigDecimal

object JsonHelper {

  implicit object JavaBigDecimalWrites extends AnyRef with Writes[java.math.BigDecimal] {
    def writes(o : java.math.BigDecimal): JsNumber = JsNumber(BigDecimal(o))
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
  implicit val tariffWrites: Writes[Tariff] = Json.writes[Tariff]


  implicit val eventWrites = Json.writes[Event]
  /*
    implicit val userReads: Reads[User] = (
      (__ \ "id").read[Long] ~
        (__ \ "email").read[String] ~
        (__ \ "login").read[String] ~
        (__ \ "password").readNullable[String] ~
        (__ \ "profile").read[UserProfile]
      )(User.apply _)

    implicit val userWrites = new Writes[User] {
      def writes(user: User) = Json.obj(
        "id" -> user.id,
        "email" -> user.email,
        "login" -> user.login,
        "app/json/JsonWriters.scala"

      99L,
      )
    }*/
}
