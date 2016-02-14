package root

import com.greencatsoft.angularjs.core.{Route, RouteProvider}
import com.greencatsoft.angularjs.{inject, Config}


object RoutingConfig extends Config {

  @inject
  var routeProvider: RouteProvider = _

  override def initialize() {

    routeProvider
      .when(
        path = "/",
        route = Route(
          templateUrl = urlTemplatePath("/"),
          title = "Main"))
      .when(
        path = "/admin/",
        route = Route(
          templateUrl = urlTemplatePath("/"),
          title = "Main"))
      .when(
        path = "/events",
        route = Route(
          templateUrl = urlTemplatePath("/events"),
          title = "Main"))
  }
  
  val urlTemplatePath = Map(
    "/" -> "/assets/templates/landingPage/landingPage.html",
    "/events" -> "/assets/templates/admin/adminEvents.html"
  )
}
