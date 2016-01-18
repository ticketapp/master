package addresses

import play.api.Logger
import play.api.data.Forms._

import scala.util.{Failure, Success, Try}


trait AddressFormsTrait {

  val addressBindingForm = mapping(
    "city" -> optional(text(2)),
    "zip" -> optional(text(2)),
    "street" -> optional(text(2))
  )(addressFormApply)(addressFormUnapply)

  def addressFormApply(city: Option[String], zip: Option[String], street: Option[String]): Option[Address] = Try {
    Address(city = city, zip = zip, street = street)
  } match {
    case Success(validAddress) =>
      Option(validAddress)
    case Failure(_) =>
      Logger.error("Address.formApply: empty address has not been created")
      None
  }

  def addressFormUnapply(maybeAddress: Option[Address]): Option[(Option[String], Option[String], Option[String])] =
    Some((maybeAddress.get.city, maybeAddress.get.zip, maybeAddress.get.street))
}
