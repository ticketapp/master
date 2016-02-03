package landingPage

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{AbstractController, injectable}
import events.Happening
import httpServiceFactory.HttpGeneralService

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("landingPageController")
class LandingPageController(landingPageScope: LandingPageScope, service: HttpGeneralService, timeout: Timeout)
  extends AbstractController[LandingPageScope](landingPageScope) {

  scope.events = new js.Array[Happening]

}
