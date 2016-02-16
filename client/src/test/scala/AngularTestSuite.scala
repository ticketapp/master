import com.greencatsoft.greenlight.{BeforeAndAfter, BeforeAndAfterAll, TestSuite}

import scala.scalajs.js
import scala.scalajs.js.GlobalScope


trait JasmineAdapter {
  this: TestSuite with BeforeAndAfter =>
}

@js.native
object JasmineAdapter extends GlobalScope {

  def beforeEachHooks: js.Array[js.ThisFunction0[Any, Unit]] = js.native

  def afterEachHooks: js.Array[js.ThisFunction0[Any, Unit]] = js.native
}

trait AngularMockTest extends TestSuite with BeforeAndAfter with BeforeAndAfterAll with JasmineAdapter {

}

@js.native
object AngularMockTest extends GlobalScope {

  def module(name: String): Unit = js.native

  def inject(dependencies: js.Array[Any]): Unit = js.native
}