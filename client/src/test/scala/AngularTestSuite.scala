
import adminClient.AdminController
import com.greencatsoft.angularjs.{Module, Angular}
import com.greencatsoft.greenlight.{BeforeAndAfterAll, BeforeAndAfter, TestSuite}
import org.scalajs.dom.window
import scala.scalajs.js
import scala.scalajs.js.GlobalScope
import scala.concurrent.ExecutionContext.Implicits.global


@js.native
trait JasmineAdapter {
  this: TestSuite with BeforeAndAfter =>
}

@js.native
object JasmineAdapter extends GlobalScope {

  def beforeEachHooks: js.Array[js.ThisFunction0[Any, Unit]] = js.native

  def afterEachHooks: js.Array[js.ThisFunction0[Any, Unit]] = js.native
}

@js.native
trait AngularMockTest extends TestSuite with BeforeAndAfter with BeforeAndAfterAll with JasmineAdapter {
  /*
    import AngularMockTest._
    import JasmineAdapter._

    override def name: String = "app"



    private var moduleCallbacks: Seq[Function1[Module, Unit]] = Seq()

    beforeAll {
      println("module")
      val module = Angular.module(name, modules)
      println(module)
      moduleCallbacks foreach {
        callback => callback(module)
      }
    }

  /*  before {
      beforeEachHooks.foreach(_(window))

      module(name)
    }

    after {
      afterEachHooks.foreach(_(window))
    }*/

    def withModule(callback: Module => Unit) {
      println(callback)
      this.moduleCallbacks :+= callback
    }

    /*def withService[A](name: String)(callback: A => Unit): Unit = {
      val handler: js.Function1[A, Unit] = callback

      inject(js.Array(name, handler))
    }*/
    def withController[A](name: String)(callback: A => Unit): Unit = {
      println(callback)
      println(name)
      val handler: js.Function1[A, Unit] = callback

      inject(js.Array(name, handler))
    }*/
}

object AngularMockTest extends GlobalScope {

  def module(name: String): Unit = js.native

  def inject(dependencies: js.Array[Any]): Unit = js.native
}