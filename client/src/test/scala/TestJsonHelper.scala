import admin.{TicketStatus, Ticket}
import artists.Artist
import events.{Geometry, Happening}
import organizers.Organizer
import places.Place
import upickle.Js
import upickle.default._
import utilities.jsonHelper

import scala.scalajs.js
import scala.scalajs.js.{Date, JSON}

@js.native
object TestJsonHelper extends AngularMockTest with jsonHelper {

  val expectedArtist = Artist(
    id = Some(2),
    facebookId = Some("230180803665585"),
    name = "dephas8",
    imagePath = Some("https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg"),
    description = Some("<div class='column large-12'>Artiste Dijonnais</div>"),
    facebookUrl = "dephas8",
    websites = Set("soundcloud.com/dephas8", "myspace.com/dephas8", "twitter.com/dephas8", "hiqdub.net-label.fr"),
    hasTracks = true,
    likes = None,
    country = None
  )

  val expectedEvent = Happening(
    id = Some(2),
    facebookId = Some("230180803665585"),
    name = "dephas8",
    imagePath = Some("https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg"),
    description = Some("<div class='column large-12'>Artiste Dijonnais</div>"),
    isPublic = true,
    isActive = true,
    geographicPoint = Geometry("0.0"),
    startTime = new Date(1),
    endTime = None,
    ageRestriction = 16,
    tariffRange = None,
    ticketSellers = None
  )

  val expectedPlace = Place(
    id = Some(2),
    facebookId = Some("230180803665585"),
    name = "dephas8",
    imagePath = Some("https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg"),
    description = Some("<div class='column large-12'>Artiste Dijonnais</div>"),
    geographicPoint = Geometry("0.0"),
    websites = None,
    capacity = Some(400),
    openingHours = None,
    addressId = None,
    linkedOrganizerId = None
  )

  val expectedOrganizer = Organizer(
    id = Some(2),
    facebookId = Some("230180803665585"),
    name = "dephas8",
    imagePath = Some("https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg"),
    description = Some("<div class='column large-12'>Artiste Dijonnais</div>"),
    geographicPoint = Geometry("0.0"),
    websites = None,
    addressId = None,
    linkedPlaceId = None,
    phone = None,
    publicTransit = None,
    verified = true
  )

  val expectedTicket = Ticket(
    ticketId = Some(1),
    qrCode = "qrCode",
    eventId = 1,
    tariffId = 1
  )

  val expectedTicketStatus = TicketStatus(
    ticketId = 1,
    status = 'a',
    date = new Date(1)
  )


  "The JsonHelper" should "read an artist" in {
    val stringArtist = "{\"id\":2," +
      "\"facebookId\":\"230180803665585\",\"name\":\"dephas8\"," +
      "\"imagePath\":\"https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg\"" +
      ",\"description\":\"<div class='column large-12'>Artiste Dijonnais</div>\",\"facebookUrl\":\"dephas8\",\"websites\":[\"soundcloud.com/dephas8\",\"myspace.com/dephas8\"," +
      "\"twitter.com/dephas8\",\"hiqdub.net-label.fr\"],\"hasTracks\":true}"
    val artistJson = JSON.parse(stringArtist)

    val readArtist = read[Artist](JSON.stringify(artistJson))

    readArtist must be(expectedArtist)
  }

  "The JsonHelper" should "read an event" in {
    val stringEvent =
      """
       {
        "id": 2,
        "facebookId": "230180803665585",
        "name": "dephas8",
        "imagePath": "https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg",
        "description": "<div class='column large-12'>Artiste Dijonnais</div>",
        "isPublic": true,
        "isActive": true,
        "geographicPoint": "0.0",
        "startTime": 1,
        "ageRestriction": 16
       }
      """
    val eventJson = JSON.parse(stringEvent)

    val readEvent = read[Happening](JSON.stringify(eventJson))

    readEvent must be(expectedEvent.copy(startTime = readEvent.startTime))
  }

  "The JsonHelper" should "read a place" in {
    val stringPlace =
      """
       {
        "id": 2,
        "facebookId": "230180803665585",
        "name": "dephas8",
        "imagePath": "https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg",
        "description": "<div class='column large-12'>Artiste Dijonnais</div>",
        "geographicPoint": "0.0",
        "capacity": 400
       }
      """
    val placeJson = JSON.parse(stringPlace)

    val readPlace = read[Place](JSON.stringify(placeJson))

    readPlace must be(expectedPlace)
  }
  

  "The JsonHelper" should "read an organizer" in {
    val stringOrganizer =
      """
       {
        "id": 2,
        "facebookId": "230180803665585",
        "name": "dephas8",
        "imagePath": "https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg",
        "description": "<div class='column large-12'>Artiste Dijonnais</div>",
        "geographicPoint": "0.0",
        "verified": true
       }
      """
    val organizerJson = JSON.parse(stringOrganizer)

    val readOrganizer = read[Organizer](JSON.stringify(organizerJson))

    readOrganizer must be(expectedOrganizer)
  }


  "The JsonHelper" should "read a ticket" in {
    val stringTicket =
      """
       {
       "ticketId": 1,
       "qrCode": "qrCode",
       "eventId": 1,
       "tariffId": 1
       }

      """
    val ticketJson = JSON.parse(stringTicket)

    val readTicket = read[Ticket](JSON.stringify(ticketJson))
    
    readTicket must be(expectedTicket)
  }

  "The JsonHelper" should "read a ticket" in {
    val stringTicketStatus =
      """
      {
        "ticketId": 1,
        "status": "a",
        "date": 1
      }

      """
    val ticketStatusJson = JSON.parse(stringTicketStatus)

    val readTicketStatus = read[TicketStatus](JSON.stringify(ticketStatusJson))

    readTicketStatus must be(expectedTicketStatus.copy(date = readTicketStatus.date))
  }


  "getOptionLong" should "transform a maybe existing field to an option of Long" in {
    val jsLong = Js.Num(1)
    val map = Map("long" -> jsLong, "b" -> Js.Str("other value"))
    val mapWithoutLong = Map("b" -> Js.Str("other value"))

    getOptionLong(map, "long") must be(Some(1))
    getOptionLong(mapWithoutLong, "long") must be(None)
  }

  "getOptionInt" should "transform a maybe existing field to an option of Int" in {
    val jsInt = Js.Num(1)
    val map = Map("int" -> jsInt, "b" -> Js.Str("other value"))
    val mapWithoutInt = Map("b" -> Js.Str("other value"))

    getOptionInt(map, "int") must be(Some(1))
    getOptionInt(mapWithoutInt, "int") must be(None)
  }

  "getOptionString" should "transform a maybe existing field to an option of String" in {
    val jsString = Js.Str("String")
    val map = Map("string" -> jsString, "b" -> Js.Str("other value"))
    val mapWithoutString = Map("b" -> Js.Str("other value"))

    getOptionString(map, "string") must be(Some("String"))
    getOptionString(mapWithoutString, "string") must be(None)
  }

  "getOptionDate" should "transform a maybe existing field to an option of Date" in {
    val jsDate = Js.Num(1)
    val map = Map("date" -> jsDate, "b" -> Js.Str("other value"))
    val mapWithoutDate = Map("b" -> Js.Str("other value"))

    getOptionDate(map, "date").get.getDate() must be(new Date(1).getDate())
    getOptionDate(mapWithoutDate, "date") must be(None)
  }
}