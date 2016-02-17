package addresses

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Address(id: Option[Long] = None,
                   geographicPoint: String,
                   city: Option[String] = None,
                   zip: Option[String] = None,
                   street: Option[String] = None)
