import com.greencatsoft.greenlight.{TestSuite, BeforeAndAfter}

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