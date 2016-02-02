import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js


@js.native
trait TestScope extends Scope {

  var id: String = js.native
  var name: String = js.native
  var email: String = js.native
  var friends: js.Array[String] = js.native

  var delete: js.Function = js.native
}

@js.native
object TestAdminClientController extends AngularMockTest {

  "an admin client controller" should
    "get all salable events" in {
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
    /*withController[App]("adminController") { controller =>
        println("jkjskjkskdjkkdsjdk" + controller)
        val events = controller
        events should not be empty
      }
    withModule { module =>
      println(module)
    }*/
  }
  "The framework" should "be able to test equality" in {

    (1 + 1) must be (2)

    ("A" + "B") should be ("AB")

    "Scala.js" must not be "overlooked!"
  }
}
