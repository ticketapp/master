package utilities

import addresses.Address
import admin.{Ticket, TicketStatus, TicketWithStatus}
import artists.Artist
import events.{Geometry, Happening}
import organizers.Organizer
import places.{Place, PlaceWithAddress}
import upickle.Js
import upickle.Js.{Num, Str, Value}
import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js.Date


trait jsonHelper {

  implicit val dateTimeWriter = upickle.default.Writer[Date] {
    case date => Js.Str(date.toString)
  }

  implicit val dateTimeReader = upickle.default.Reader[Date] {
    case Js.Str(str) => new Date(str)
    case a => new Date(a.value.toString.toLong)
  }

  def getOptionLong(map: Map[String, Any], key: String): Option[Long] = {
    if (!map.isDefinedAt(key)) None else Some(map(key).asInstanceOf[Js.Num].value.toLong)
  }

  def getOptionInt(map: Map[String, Any], key: String): Option[Int] = {
    if (!map.isDefinedAt(key)) None else Some(map(key).asInstanceOf[Js.Num].value.toInt)
  }

  def getOptionString(map: Map[String, Any], key: String): Option[String] = {
    if (!map.isDefinedAt(key)) None else Some(map(key).asInstanceOf[Js.Str].value.toString)
  }

  def getOptionDate(map: Map[String, Any], key: String): Option[Date] = {
    if (!map.isDefinedAt(key)) None else Some(new Date(map(key).asInstanceOf[Js.Num].value))
  }

  implicit val eventReader = upickle.default.Reader[Happening]{
      case other =>
        jsEventToEvent(other)
    }

  def jsEventToEvent(other: Value): Happening = {
    val event = other.value.asInstanceOf[ArrayBuffer[Pair[String, Any]]].toMap
    Happening(
      id = getOptionLong(event, "id"),
      facebookId = getOptionString(event, "facebookId"),
      isPublic = event("isPublic").toString.toBoolean,
      isActive = event("isActive").toString.toBoolean,
      name = event("name").asInstanceOf[Str].value.toString,
      geographicPoint = Geometry(point = event("geographicPoint").toString),
      description = getOptionString(event, "description"),
      startTime = new Date(event("startTime").asInstanceOf[Js.Num].value),
      endTime = getOptionDate(event, "endTime"),
      ageRestriction = event("ageRestriction").asInstanceOf[Num].value.toInt,
      tariffRange = getOptionString(event, "tariffRange"),
      ticketSellers = getOptionString(event, "ticketSellers"),
      imagePath = getOptionString(event, "imagePath")
    )
  }

  implicit val placeReader = upickle.default.Reader[Place]{
    case placeObject =>
      placeJsValueToPlace(placeObject)
  }

  def placeJsValueToPlace(placeObject: Value): Place = {
    val place: Map[String, Any] = placeObject.value.asInstanceOf[ArrayBuffer[Pair[String, Any]]].toMap
    Place(
      id = getOptionLong(place, "id"),
      name = place("name").asInstanceOf[Str].value.toString,
      facebookId = getOptionString(place, "facebookId"),
      geographicPoint = Geometry(point = place("geographicPoint").asInstanceOf[Str].value.toString),
      description = getOptionString(place, "description"),
      websites = getOptionString(place, "websites"),
      capacity = getOptionInt(place, "capacity"),
      imagePath = getOptionString(place, "imagePath"),
      openingHours = getOptionString(place, "openingHours"),
      addressId = getOptionLong(place, "addressId"),
      linkedOrganizerId = getOptionLong(place, "linkedOrganizerId")
    )
  }

  implicit val organizerReader = upickle.default.Reader[Organizer]{
    case organizerObject =>
      val organizer: Map[String, Any] = organizerObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
      Organizer(
        id = getOptionLong(organizer, "id"),
        facebookId = getOptionString(organizer, "facebookId"),
        name = organizer("name").asInstanceOf[Js.Str].value.toString,
        description = getOptionString(organizer, "description"),
        addressId = getOptionLong(organizer, "addressId"),
        phone = getOptionString(organizer, "phone"),
        publicTransit = getOptionString(organizer, "publicTransit"),
        websites = getOptionString(organizer, "websites"),
        verified = organizer("verified").toString.toBoolean,
        imagePath = getOptionString(organizer, "imagePath"),
        geographicPoint = Geometry(point = organizer("geographicPoint").asInstanceOf[Js.Str].value.toString),
        linkedPlaceId = getOptionLong(organizer, "linkedPlaceId")
      )
  }

  implicit val artistReader = upickle.default.Reader[Artist]{
    case artistObject =>
     val artist: Map[String, Any] = artistObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
      Artist(
        id = getOptionLong(artist, "id"),
        facebookId = getOptionString(artist, "facebookId"),
        name = artist("name").asInstanceOf[Js.Str].value.toString,
        imagePath = getOptionString(artist, "imagePath"),
        description = getOptionString(artist, "description"),
        facebookUrl = artist("facebookUrl").asInstanceOf[Js.Str].value.toString,
        websites = artist("websites").asInstanceOf[Js.Arr].value.toSet.map{ws: Js.Value => ws.value.toString},
        hasTracks = artist("hasTracks").toString.toBoolean,
        likes = getOptionInt(artist, "likes"),
        country = getOptionString(artist, "country")
      )
  }

  implicit val addressReader = upickle.default.Reader[Address]{
    case addressObject =>
      jsAddressToAddress(addressObject)
  }

  def jsAddressToAddress(addressObject: Value): Address = {
    val address: Map[String, Any] = addressObject.value.asInstanceOf[ArrayBuffer[Pair[String, Any]]].toMap
    Address(
      id = getOptionLong(address, "id"),
      geographicPoint = Geometry(point = address("geographicPoint").asInstanceOf[Str].value.toString),
      city = getOptionString(address, "city"),
      zip = getOptionString(address, "zip"),
      street = getOptionString(address, "street")
    )
  }

  implicit val placeWithAddressReader = upickle.default.Reader[PlaceWithAddress]{
    case placeObject =>
      val placeWithRelation: Map[String, Any] = placeObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
      PlaceWithAddress(
        place = placeJsValueToPlace(placeWithRelation("place").asInstanceOf[Js.Value]),
        maybeAddress = if(placeWithRelation.isDefinedAt("maybeAddress"))
        Some(jsAddressToAddress(placeWithRelation("maybeAddress").asInstanceOf[Js.Value])) else None
      )
  }

  def jsTicketToTicket(ticketObject: Value): Ticket = {
    val ticket: Map[String, Any] = ticketObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
    Ticket(
      ticketId = getOptionInt(ticket, "ticketId"),
      qrCode = ticket("qrCode").asInstanceOf[Str].value.toString,
      eventId = ticket("eventId").asInstanceOf[Js.Num].value.toInt,
      tariffId = ticket("tariffId").asInstanceOf[Js.Num].value.toInt
    )
  }

  def jsTicketStatusToTicketStatus(ticketObject: Value): TicketStatus = {
    val ticket: Map[String, Any] = ticketObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
    TicketStatus(
      ticketId = ticket("ticketId").asInstanceOf[Js.Num].value.toInt,
      status = ticket("status").asInstanceOf[Js.Str].value.toString.charAt(0),
      date = new Date(ticket("date").asInstanceOf[Js.Num].value)
    )
  }

  implicit val ticketReader = upickle.default.Reader[Ticket]{
     case ticketObject =>
      jsTicketToTicket(ticketObject)
  }

  implicit val ticketStatusReader = upickle.default.Reader[TicketStatus]{
     case ticketObject =>
      jsTicketStatusToTicketStatus(ticketObject)
  }

  implicit val ticketWithStatusReader = upickle.default.Reader[TicketWithStatus]{
    case ticketObject =>
      val ticketWithRelation: Map[String, Any] = ticketObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
     TicketWithStatus(ticket= jsTicketToTicket(ticketObject("ticket")),
     ticketStatus= if(ticketWithRelation.isDefinedAt("ticketStatus"))
     Some(jsTicketStatusToTicketStatus(ticketObject("ticketStatus"))) else None
     )
  }

}