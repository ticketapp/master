import java.util.Date

import admin.{Ticket, TicketStatus}
import artists.Artist
import events.Happening
import organizers.Organizer
import places.Place
import upickle.default._
import utilities.jsonHelper


object TestJsonHelper extends AngularMockTest with jsonHelper {

  "A Json artist" should "be read as an Artist" in {
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

    val stringArtist = """{
                      "id":2,"facebookId":"230180803665585","name":"dephas8",
                      "imagePath":"https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg",
                      "description":"<div class='column large-12'>Artiste Dijonnais</div>","facebookUrl":"dephas8",
                      "websites":["soundcloud.com/dephas8","myspace.com/dephas8","twitter.com/dephas8",
                      "hiqdub.net-label.fr"],"hasTracks":true
                      }"""
    val readArtist = read[Artist](stringArtist)

    readArtist must be (expectedArtist)
  }

  "A Json event" should "be read as an Event" in {
    val expectedEvent = Happening(
      id = Some(2),
      facebookId = Some("230180803665585"),
      name = "dephas8",
      imagePath = Some("https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg"),
      description = Some("<div class='column large-12'>Artiste Dijonnais</div>"),
      isPublic = true,
      isActive = true,
      geographicPoint = "0.0,5",
      startTime = new Date(1457128800000L),
      endTime = None,
      ageRestriction = 16,
      tariffRange = None,
      ticketSellers = None
    )

    val stringEvent =
      s"""
       {
        "id": 2,
        "facebookId": "230180803665585",
        "name": "dephas8",
        "imagePath": "https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg",
        "description": "<div class='column large-12'>Artiste Dijonnais</div>",
        "isPublic": true,
        "isActive": true,
        "geographicPoint": "0.0,5",
        "startTime": 1457128800000,
        "ageRestriction": 16
       }
      """

    val readEvent = read[Happening](stringEvent)

    expectedEvent.startTime must be (readEvent.startTime)

    readEvent must be (expectedEvent)
  }

  "A json place" should "be read as a Place" in {
    val expectedPlace = Place(
      id = Some(2),
      facebookId = Some("230180803665585"),
      name = "dephas8",
      imagePath = Some("https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg"),
      description = Some("<div class='column large-12'>Artiste Dijonnais</div>"),
      geographicPoint = "0.0",
      websites = None,
      capacity = Some(400),
      openingHours = None,
      addressId = None,
      linkedOrganizerId = None
    )

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

    val readPlace = read[Place](stringPlace)

    readPlace must be (expectedPlace)
  }


  "A Json organizer" should "read as an Organizer" in {
    val expectedOrganizer = Organizer(
      id = Some(2),
      facebookId = Some("230180803665585"),
      name = "dephas8",
      imagePath = Some("https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg"),
      description = Some("<div class='column large-12'>Artiste Dijonnais</div>"),
      geographicPoint = "0.0",
      websites = None,
      addressId = None,
      linkedPlaceId = None,
      phone = None,
      publicTransit = None,
      verified = true
    )

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
    val readOrganizer = read[Organizer](stringOrganizer)

    readOrganizer must be (expectedOrganizer)
  }


  "A json ticket" should "be read as a Ticket" in {
    val expectedTicket = Ticket(
      ticketId = Some(1),
      qrCode = "qrCode",
      eventId = 1,
      tariffId = 1
    )

    val stringTicket =
      """
       {
       "ticketId": 1,
       "qrCode": "qrCode",
       "eventId": 1,
       "tariffId": 1
       }
      """
    val readTicket = read[Ticket](stringTicket)

    readTicket must be (expectedTicket)
  }

  "A json ticketStatus" should "be read as a TicketStatus" in {
    val expectedTicketStatus = TicketStatus(
      ticketId = 1,
      status = 'a',
      date = new Date(1)
    )

    val stringTicketStatus =
      """
      {
        "ticketId": 1,
        "status": "a",
        "date": 1
      }
      """
    val readTicketStatus = read[TicketStatus](stringTicketStatus)

    readTicketStatus must be (expectedTicketStatus)
  }
}