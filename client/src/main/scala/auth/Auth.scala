package auth

import com.greencatsoft.angularjs.core.Promise
import com.greencatsoft.angularjs.extensions.material.BottomSheetHideOrCancel
import com.greencatsoft.angularjs.injectable

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@js.native
@injectable("$auth")
trait Auth extends  js.Object {
  def authenticate(provider: String) = js.native
}

@js.native
@injectable("$authProvider")
trait AuthProvider extends  js.Object {
  def facebook(provider: Provider) = js.native
}

trait Provider extends js.Object {
  var clientId: String = js.native
}