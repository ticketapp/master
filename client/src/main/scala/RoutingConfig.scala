import com.greencatsoft.angularjs.core.{Route, RouteProvider}
import com.greencatsoft.angularjs.{inject, Config}

object RoutingConfig extends Config {

  @inject
  var routeProvider: RouteProvider = _

  override def initialize(): Unit = {

    routeProvider
      .when(
        path = "/",
        route = Route(
          templateUrl = "/assets/templates/landingPage/landingPage.html",
          title = "Main"))
      .when(
        path = "/admin/",
        route = Route(
          templateUrl = "/assets/templates/landingPage/landingPage.html",
          title = "Admin"))
      .when(
        path = "/adminEvents",
        route = Route(
          templateUrl = "/assets/templates/admin/adminEvents.html",
          title = "AdminEvents"))
      .when(
        path = "/events/:id",
        route = Route(
          templateUrl = "/assets/templates/events/event.html",
          title = "Events"))      
      .when(
        path = "/organizers/:id",
        route = Route(
          templateUrl = "/assets/templates/organizers/organizer.html",
          title = "Organizers"))      
      .when(
        path = "/places/:id",
        route = Route(
          templateUrl = "/assets/templates/places/place.html",
          title = "Places"))
      .when(
        path = "/sellTicket/:eventId",
        route = Route(
          templateUrl = "/assets/templates/sellTicket/sellTicket.html",
          title = "sellTicket"))
      .when(
        path = "/cookies",
        route = Route(
          templateUrl = "assets/templates/cookies/cookies.html",
          title = "cookies"))
      .when(
          path = "/legalNotice",
          route = Route(
          templateUrl = "/assets/templates/legalNotice/legalNotice.html",
          title = "legalNotice"))
  }
}
