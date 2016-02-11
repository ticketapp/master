package utilities

import com.greencatsoft.angularjs.injectable

import scala.scalajs.js


@js.native
@injectable("$cookies")
trait NgCookies extends js.Object {
  def get(key: String): js.Any = js.native
  def put(key: String, value: js.Any) = js.native
  def put(key: String, value: js.Any, options: CookiesOptions) = js.native
}

trait CookiesOptions extends js.Object {
  var expires: js.Date = js.native
}