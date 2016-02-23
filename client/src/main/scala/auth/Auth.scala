package auth

import com.greencatsoft.angularjs.injectable

import scala.scalajs.js

@js.native
@injectable("$auth")
trait Auth extends  js.Object {
  def authenticate(provider: String): js.Any = js.native
}

@js.native
@injectable("$authProvider")
trait AuthProvider extends  js.Object {
  def facebook(provider: Provider): js.Any = js.native
}

@js.native
trait Provider extends js.Object {
  var clientId: String = js.native
}