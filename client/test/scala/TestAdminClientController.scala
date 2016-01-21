import com.greencatsoft.greenlight.TestSuite

object TestAdminClientController extends TestSuite {

  /*"an admin client controller" should
    "get all salable events" in {
      withController[AdminController]("getSalableEvents") { controller =>
        val events = Await.result(controller.getSalableEvents, 5 seconds )
        events should not be empty
      }
    }*/
  "The framework" should "be able to test equality" in {

    (1 + 1) must be (2)

    ("A" + "B") should be ("AB")

    "Scala.js" must not be ("overlooked!")
  }


}
