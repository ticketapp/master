import addresses.Address
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import testsHelper.GlobalApplicationForModels


class TestAddressModel extends GlobalApplicationForModels {

  "An address" must {

    "not be created if empty" in {

      Address(id = None, city = Some("jkl"), zip = None, street = None)
      Address(id = None, city = None, zip = Some("jkl"), street = None)
      Address(id = None, city = None, zip = None, street = Some("jkl"))

      an[java.lang.IllegalArgumentException] should be thrownBy Address(
        id = None,
        geographicPoint = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
        city = None,
        zip = None,
        street = None)
    }
  }
}
