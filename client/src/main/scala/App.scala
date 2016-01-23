import AdminClient.AdminController
import com.greencatsoft.angularjs._
import Contact.{ContactController, ContactComponentDirective}
import httpServiceFactory.HttpGeneralServiceFactory
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport


@JSExport
object App extends JSApp {

  override def main() {
    val module = Angular.module("app", Seq("ngAnimate", "ngAria", "ngMaterial", "mm.foundation", "ngRoute", "ngMap",
      "websocketService"))

    module.config(RoutingConfig)
    module.controller[ContactController]
    module.directive[ContactComponentDirective]
    module.factory[HttpGeneralServiceFactory]
    module.controller[AdminController]
  }
}
