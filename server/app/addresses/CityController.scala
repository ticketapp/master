package addresses

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global


class CityController @Inject()(val cityMethods: CityMethods) extends Controller {

  def isACity(pattern: String) = Action.async {
    cityMethods.isACity(pattern) map { isACity =>
      Ok(Json.toJson(isACity))
    }
  }
}
