package Addresses

import events.Geometry

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Address(id: Option[Long],
                   geographicPoint: Geometry,
                   city: Option[String],
                   zip: Option[String],
                   street: Option[String])
