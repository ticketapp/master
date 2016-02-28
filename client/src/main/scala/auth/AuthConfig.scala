package auth

import com.greencatsoft.angularjs.{Config, inject}

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