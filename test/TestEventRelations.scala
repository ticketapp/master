import addresses.Address
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import eventsDomain.{Event, EventWithRelations}
import org.joda.time.DateTime
import placesDomain.{PlaceWithAddress, Place}
import services.Utilities
import testsHelper.GlobalApplicationForModels


class TestEventRelations extends GlobalApplicationForModels with Utilities {

  "EventWithRelations" must {

    "set the good geographicPoint depending of its relations" in {
      val somewhereNotInAntarctic = new GeometryFactory().createPoint(new Coordinate(-5, 5))

      val eventWithoutGeoPoint = Event(
        isPublic = true,
        isActive = true,
        name = "nameEventRelations",
        startTime = new DateTime(),
        endTime = None,
        ageRestriction = 16,
        tariffRange = None,
        ticketSellers = None,
        imagePath = None)
      val event2 = eventWithoutGeoPoint.copy(geographicPoint = somewhereNotInAntarctic)
      val eventWithAntarcticGeoPoint = eventWithoutGeoPoint.copy(geographicPoint = antarcticPoint)
      val placesWithoutGeoPoints = Seq(PlaceWithAddress(Place(name = "name")))
      val places2 = Seq(PlaceWithAddress(Place(name = "name", geographicPoint = somewhereNotInAntarctic)))
      val places3 = Seq(PlaceWithAddress(Place(name = "name"), Option(Address(geographicPoint = somewhereNotInAntarctic))))
      val addresses1 = Seq(Address(geographicPoint = somewhereNotInAntarctic))
      val addresses2 = Seq(Address(city = Option("city")), Address(geographicPoint = somewhereNotInAntarctic))
      val addressesWithoutGeoPoints = Seq(Address(city = Option("city")))

      EventWithRelations(
        event = eventWithoutGeoPoint,
        places = placesWithoutGeoPoints,
        addresses = addressesWithoutGeoPoints).geographicPoint mustBe antarcticPoint

      EventWithRelations(
        event = eventWithAntarcticGeoPoint,
        places = placesWithoutGeoPoints,
        addresses = addresses1).geographicPoint mustBe somewhereNotInAntarctic

      EventWithRelations(
        event = eventWithAntarcticGeoPoint,
        places = places3,
        addresses = addressesWithoutGeoPoints).geographicPoint mustBe somewhereNotInAntarctic

      EventWithRelations(
        event = eventWithAntarcticGeoPoint,
        places = places3,
        addresses = addresses1).geographicPoint mustBe somewhereNotInAntarctic

      EventWithRelations(
        event = eventWithoutGeoPoint,
        places = placesWithoutGeoPoints,
        addresses = addresses1).geographicPoint mustBe somewhereNotInAntarctic

      EventWithRelations(
        event = eventWithoutGeoPoint,
        places = placesWithoutGeoPoints,
        addresses = addresses2).geographicPoint mustBe somewhereNotInAntarctic

      EventWithRelations(
        event = eventWithoutGeoPoint,
        places = places2,
        addresses = addressesWithoutGeoPoints).geographicPoint mustBe somewhereNotInAntarctic

      EventWithRelations(
        event = eventWithoutGeoPoint,
        places = places3,
        addresses = addressesWithoutGeoPoints).geographicPoint mustBe somewhereNotInAntarctic

      EventWithRelations(event = event2).geographicPoint mustBe somewhereNotInAntarctic
    }
  }
}
