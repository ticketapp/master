package models

import play.api.Logger
import play.api.data.Forms._


trait addressFormsTrait {

  val addressBindingForm = mapping(
    "city" -> optional(text(2)),
    "zip" -> optional(text(2)),
    "street" -> optional(text(2))
  )(addressFormApply)(addressFormUnapply)

  def addressFormApply(city: Option[String], zip: Option[String], street: Option[String]): Option[Address] = try {
    Option(Address(None, None, city, zip, street))
  } catch {
    case e: Exception =>
      Logger.error("Address.formApply: empty address has not been created")
      None
  }

  def addressFormUnapply(maybeAddress: Option[Address]): Option[(Option[String], Option[String], Option[String])] =
    Some((maybeAddress.get.city, maybeAddress.get.zip, maybeAddress.get.street))
}
