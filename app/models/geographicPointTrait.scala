package models

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Geometry}
import controllers.OverQueryLimit
import play.api.Logger
import play.api.libs.ws.WSResponse

import scala.util.{Failure, Success, Try}

trait geographicPointTrait {
  def readGoogleGeographicPoint(googleGeoCodeResponse: WSResponse): Try[Option[Geometry]] = googleGeoCodeResponse.statusText match {
    case "OK" =>
      val googleGeoCodeJson = googleGeoCodeResponse.json
      val latitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lat").asOpt[Double]
      val longitude = ((googleGeoCodeJson \ "results")(0) \ "geometry" \ "location" \ "lng").asOpt[Double]
      latitude match {
        case None =>
          Success(None)
        case Some(lat) =>
          longitude match {
            case None =>
              Success(None)
            case Some(lng) =>
              latAndLngToGeographicPoint(lat, lng) match {
                case Success(point) =>
                  Success(Option(point))
                case Failure(e) =>
                  Logger.error("GeographicPointMethods.readGoogleGeographicPoint: ", e)
                  Success(None)
              }
          }
      }
    case "OVER_QUERY_LIMIT" =>
      Failure(new OverQueryLimit("GeographicPointMethods.readGoogleGeographicPoint"))
    case otherStatus =>
      Failure(new Exception(otherStatus))
  }

  val geometryFactory = new GeometryFactory()

  def stringToGeographicPoint(string: String): Try[Geometry] = {
    val latitudeAndLongitude: Array[String] = string.split(",")
    latAndLngToGeographicPoint(latitudeAndLongitude(0).toDouble, latitudeAndLongitude(1).toDouble)
  }

  def latAndLngToGeographicPoint(latitude: Double, longitude: Double): Try[Geometry] = Try {
    val coordinate= new Coordinate(latitude, longitude)
    geometryFactory.createPoint(coordinate)
  }

  def optionStringToOptionPoint(maybeGeographicPoint: Option[String]): Option[Geometry] = maybeGeographicPoint match {
    case None =>
      None
    case Some(geoPoint) =>
      stringToGeographicPoint(geoPoint) match {
        case Failure(exception) =>
          Logger.error("Utilities.optionStringToPoint: ", exception)
          None
        case Success(validPoint) =>
          Some(validPoint)
      }
  }

  def readFacebookGeographicPoint() = {
    //   val geographicPoint = (eventJson \ "venue" \ "latitude").as[Option[Float]] match {
    //     case Some(latitude) =>
    //       (eventJson \ "venue" \ "longitude").as[Option[Float]] match {
    //         case Some(longitude) => Some(s"($latitude,$longitude)")
    //         case _ => None
    //       }
    //     case _ => None
    //   }
  }
}
