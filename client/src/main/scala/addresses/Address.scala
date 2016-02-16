package addresses

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Address(id: Option[Long],
                   geographicPoint: String,
                   city: Option[String],
                   zip: Option[String],
                   street: Option[String])
