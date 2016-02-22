import admin.AdminController
import artists.{ArtistMinFormDirective, ArtistsController}
import auth.AuthDirective
import chatContact.{ChatContactController, ContactComponentDirective}
import com.greencatsoft.angularjs._
import events._
import focusDirective.FocusDirective
import footer.LandingPageFooterDirective
import geolocation.GeolocationServiceFactory
import httpServiceFactory.HttpGeneralServiceFactory
import organizers.OrganizersController
import places.PlacesController
import player.{PlayerDirective, PlayerServiceFactory}
import root.RoutingConfig
import tracking.{TrackingDirective, TrackingViewDirective}

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport
object App extends JSApp {

  override def main() {
    val module = Angular.module("app", Seq("ngAnimate", "ngAria", "ngMaterial", "mm.foundation", "ngRoute", "ngMap",
      "ngCookies", "angularTranslateApp", "ngSanitize", "themingAngularMaterial", "satellizer"))

    module.config(RoutingConfig)
    module.config(AuthConfig)
    module.directive[ContactComponentDirective]
    module.factory[HttpGeneralServiceFactory]
    module.factory[GeolocationServiceFactory]
    module.factory[PlayerServiceFactory]
    module.controller[AdminController]
    module.controller[EventsController]
    module.controller[ArtistsController]
    module.controller[OrganizersController]
    module.controller[PlacesController]
    module.controller[ChatContactController]
    module.directive[EventMinDirective]
    module.directive[TrackingViewDirective]
    module.directive[TrackingDirective]
    module.directive[FocusDirective]
    module.directive[EventFormFindByIdDirective]
    module.directive[EventFormFindByGeoPoint]
    module.directive[EventFormFindInHourInterval]
    module.directive[EventFormFindPassedInInterval]
    module.directive[EventFormFindAllContaining]
    module.directive[EventFormFindByCityPattern]
    module.directive[EventFormFindNearCity]
    module.directive[EventFormCreateByFacebookId]
    module.directive[ArtistMinFormDirective]
    module.directive[LandingPageFooterDirective]
    module.directive[PlayerDirective]
    module.directive[AuthDirective]
  }
}
