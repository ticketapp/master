import addresses.GeographicPointRefactoringFilter
import admin.AdminController
import artists.{ArtistMinFormDirective, ArtistsController}
import auth.{AuthConfig, AuthDirective}
import chatContact.{ChatContactController, ContactComponentDirective}
import com.greencatsoft.angularjs._
import cookies.CookiesDirective
import events._
import focusDirective.FocusDirective
import footer.LandingPageFooterDirective
import geolocation.GeolocationServiceFactory
import httpServiceFactory.HttpGeneralServiceFactory
import images.{OnErrorSrcDirective, RefactorArtistImagePathFilter}
import map.{MapController, MapControlsDirective, StylizedMapDirective}
import organizers.{OrganizerController, OrganizersController, OrganizersServiceFactory}
import places.{PlaceController, PlacesController, PlacesServiceFactory}
import player.{PlayerDirective, PlayerServiceFactory}
import sellTicket.SellTicketController
import tracking.{TrackingDirective, TrackingViewDirective}

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport
object App extends JSApp {

  override def main() {
    val module = Angular.module("app", Seq("ngAnimate", "ngAria", "ngMaterial", "mm.foundation", "ngRoute", "ngMap",
      "ngCookies", "angularTranslateApp", "ngSanitize", "themingAngularMaterial", "satellizer", "idCardUploader"))

    module.config(RoutingConfig)
    module.config(AuthConfig)
    module.directive[ContactComponentDirective]
    module.factory[HttpGeneralServiceFactory]
    module.factory[GeolocationServiceFactory]
    module.factory[EventsServiceFactory]
    module.factory[PlacesServiceFactory]
    module.factory[OrganizersServiceFactory]
    module.factory[PlayerServiceFactory]
    module.controller[AdminController]
    module.controller[EventsController]
    module.controller[ArtistsController]
    module.controller[OrganizersController]
    module.controller[OrganizerController]
    module.controller[PlacesController]
    module.controller[PlaceController]
    module.controller[ChatContactController]
    module.controller[EventController]
    module.controller[MapController]
    module.controller[SellTicketController]
    module.directive[EventMinDirective]
    module.directive[TrackingViewDirective]
    module.directive[TrackingDirective]
    module.directive[FocusDirective]
    module.directive[EventFormFindByIdDirective]
    module.directive[CookiesDirective]
    module.directive[EventFormFindByGeoPoint]
    module.directive[EventFormFindInHourInterval]
    module.directive[EventFormFindPassedInInterval]
    module.directive[EventFormFindAllContaining]
    module.directive[EventFormFindByCityPattern]
    module.directive[EventFormFindNearCity]
    module.directive[EventFormCreateByFacebookId]
    module.directive[ArtistMinFormDirective]
    module.directive[LandingPageFooterDirective]
    module.directive[StylizedMapDirective]
    module.directive[MapControlsDirective]
    module.directive[OnErrorSrcDirective]
    module.directive[EventAndPastEventsTabsDirective]
    module.filter[GeographicPointRefactoringFilter]
    module.filter[RefactorArtistImagePathFilter]
    module.directive[PlayerDirective]
    module.directive[AuthDirective]
  }
}
