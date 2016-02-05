package utilities

import addresses.Address
import artists.Artist
import events.{Geometry, Happening}
import organizers.Organizer
import places.{PlaceWithAddress, Place}
import upickle.Js
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
    if (!map.isDefinedAt(key)) None else Some(new Date(map(key).toString))
  }

  implicit val eventReader = upickle.default.Reader[Happening]{
      case other =>
        val event = other.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
        Happening(
          id = getOptionLong(event, "id"),
          facebookId = getOptionString(event, "facebookId"),
          isPublic = event("isPublic").toString.toBoolean,
          isActive = event("isActive").toString.toBoolean,
          name = event("name").asInstanceOf[Js.Str].value.toString,
          geographicPoint = Geometry(point = event("geographicPoint").toString),
          description = getOptionString(event, "description"),
          startTime = new Date(event("startTime").toString),
          endTime = getOptionDate(event, "endTime"),
          ageRestriction = event("ageRestriction").asInstanceOf[Js.Num].value.toInt,
          tariffRange = getOptionString(event, "tariffRange"),
          ticketSellers = getOptionString(event, "ticketSellers"),
          imagePath = getOptionString(event, "imagePath")
        )
    }

  implicit val placeReader = upickle.default.Reader[Place]{
    case placeObject =>
      val place: Map[String, Any] = placeObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
      Place(
        id = getOptionLong(place, "id"),
        name = place("name").asInstanceOf[Js.Str].value.toString,
        facebookId = getOptionString(place, "facebookId"),
        geographicPoint = Geometry(point = place("geographicPoint").asInstanceOf[Js.Str].value.toString),
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
      val address: Map[String, Any] = addressObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap
      Address(
        id = getOptionLong(address, "id"),
        geographicPoint = Geometry(point = address("geographicPoint").asInstanceOf[Js.Str].value.toString),
        city = getOptionString(address, "city"),
        zip = getOptionString(address, "zip"),
        street = getOptionString(address, "street")
      )
  }

  /*PlaceWithAddress(place: Place, maybeAddress: Option[Address])*/
 /* implicit val placeWithAddressReader = upickle.default.Reader[PlaceWithAddress]{
    case placeObject =>
      val placeWithRelation: Map[String, Any] = placeObject.value.asInstanceOf[scala.collection.mutable.ArrayBuffer[Tuple2[String, Any]]].toMap

  }*/

}