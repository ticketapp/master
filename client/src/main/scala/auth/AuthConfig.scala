import auth.{Provider, AuthProvider}
import com.greencatsoft.angularjs.core.{Route, RouteProvider}
import com.greencatsoft.angularjs.{inject, Config}

import scala.scalajs.js


object AuthConfig extends Config {

  @inject
  var authProvider: AuthProvider = _

  override def initialize(): Unit = {
    val facebookProvider = js.Object.asInstanceOf[Provider]
    facebookProvider.clientId = "1434764716814175"
    authProvider.facebook(facebookProvider)
  }

}