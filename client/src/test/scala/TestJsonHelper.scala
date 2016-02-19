import java.util.Date

import admin.{Ticket, TicketStatus}
import artists.{ArtistWithWeightedGenres, Artist}
import events.Happening
import genres.{Genre, GenreWithWeight}
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

  "A Json seq of artist with weighted genres" should "be read as an Artist with weighted genres" in {
    val expectedArtist = ArtistWithWeightedGenres(
      artist = Artist(
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
      ),
      genres = Seq(
        GenreWithWeight(
          genre = Genre(
            id = Some(1),
            name = "genre",
            icon = 'a'
          ),
          weight = 1
        )
      )
    )

    /*
    []
    */

    val stringArtistsWithWeightedGenres = """
                    [
                      {
                        "artist": {
                          "id":2,"facebookId":"230180803665585","name":"dephas8",
                          "imagePath":"https://scontent.xx.fbcdn.net/hphotos-xap1/t31.0-8/s720x720/1617708_860681690615490_917336920_o.jpg",
                          "description":"<div class='column large-12'>Artiste Dijonnais</div>","facebookUrl":"dephas8",
                          "websites":["soundcloud.com/dephas8","myspace.com/dephas8","twitter.com/dephas8",
                          "hiqdub.net-label.fr"],"hasTracks":true
                        },
                        "genres": [{
                          "genre": {
                            "id": 1,
                            "name": "genre",
                            "icon": "a"
                          },
                          "weight": 1
                        }]
                      },
                      {
                        "artist":
                          {"id":2,"facebookId":"339755068127","name":"Hunee",
                          "imagePath":"https://scontent.xx.fbcdn.net/hphotos-xfa1/t31.0-8/s720x720/10679758_10152932793163128_7689325429057939889_o.jpg\\0\\101",
                          "description":"<div class='column large-12'>hunch music all night long</div>",
                          "facebookUrl":"hunchmusic","websites":["hunchmusic.tumblr.com"],"hasTracks":true},
                        "genres":[
                          {"genre":
                            {"id":4,"name":"hunch","icon":"a"},"weight":0}
                        ]
                      },
                      {
                        "artist":
                          {"id":1,"facebookId":"170419243164260","name":"Mental TranceFuzion",
                          "imagePath":"https://scontent.xx.fbcdn.net/hphotos-xlt1/t31.0-8/s720x720/12657865_462880130584835_6342015048160923497_o.jpg\\0\\0",
                          "description":"<div class='column large-12'>[Mental TranceFuzion] a pour vocation la diffusion des musiques électroniques en Languedoc Roussillon",
                          "facebookUrl":"mentaltrancefuzion",
                          "websites":[],
                          "hasTracks":false},
                        "genres":[
                          {"genre":
                            {"id":1,"name":"drum  bass","icon":"a"},
                            "weight":0
                          },
                          {"genre":
                            {"id":2,"name":"tribe - acid tribe - tribecore","icon":"a"},
                            "weight":0
                          },
                          {"genre":
                            {"id":3,"name":"prog - psytrance ...etc.","icon":"a"},
                            "weight":0
                          }
                        ]
                      },
                      {
                        "artist":
                          {"id":4,"facebookId":"472005849603339","name":"Quarante&un",
                          "imagePath":"https://scontent.xx.fbcdn.net/hphotos-xfa1/t31.0-8/q85/s720x720/12694820_722676291202959_3906988724987526564_o.jpg\\0\\0",
                          "facebookUrl":"unquarante","websites":["soundcloud.com/quarante-un"],"hasTracks":true},
                        "genres":[
                          {
                          "genre":{
                            "id":5,"name":"house","icon":"a"},
                            "weight":0
                          },
                          {
                          "genre":{
                            "id":6,"name":"techno","icon":"a"},
                            "weight":0
                          }
                        ]
                      },
                      {
                      "artist":
                        {
                          "id":3,"facebookId":"203558383092250","name":"Patrice Scott",
                          "imagePath":"https://scontent.xx.fbcdn.net/hphotos-xap1/v/t1.0-0/p180x540/1934943_865574…2469453756964_n.jpg?oh=525376b62f26f03247553441ec6e833b&oe=572BBD59\\0\\50",
                          "facebookUrl":"patricescottsistrum","websites":["sistrummusic.net"],
                          "hasTracks":true
                        },
                        "genres":[]
                      }
                    ]"""
    val readArtist = read[Seq[ArtistWithWeightedGenres]](stringArtistsWithWeightedGenres)

    readArtist must contain (expectedArtist)
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