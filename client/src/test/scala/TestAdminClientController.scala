import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

object TestAdminClientController extends AngularMockTest {

//  "An admin client controller" should
//    "get all salable events" in {
//      println(new AdminController(.core.Scope, null, null).test)
//    beforeAll {
     /* val module = Angular.module("app", Seq("ngAnimate", "ngAria", "ngMaterial", "mm.foundation", "ngRoute", "ngMap", "websocketService"))
      module.controller[AdminController]
      module.factory[HttpGeneralServiceFactory]
    }
*/

     /* val httpService = new js.Object().asInstanceOf[HttpService]
      val service = new HttpGeneralService(httpService)
      val routes = js.Object.asInstanceOf[AdminRoutes]
    val scope = new js.Object().asInstanceOf[Scope]
      val controller = new AdminController(scope, service, routes)
      println("!!!!")
      println(controller.test)

      httpService.get("/salableEvents").success((a: js.Any) => {
        println(a)
      })*/
      /*controller.getSalableEvents map { a=>
      println(a)
      }*/
     /* AngularMockTest.inject(js.Array("adminController", (adminController: AdminController) => {
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }))*/
//  }
}
