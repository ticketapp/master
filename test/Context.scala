/*
//package test

import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{LoginInfo, Environment}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import models.User
import net.codingwell.scalaguice.ScalaModule
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.ExecutionContext.Implicits.global

trait Context extends Scope {

  /**
   * A fake Guice module.
   */
  class FakeModule extends AbstractModule with ScalaModule {
    def configure() = {
      bind[Environment[User, CookieAuthenticator]].toInstance(env)
    }
  }

  /**
   * An identity.
   */
  val identity = User(
    uuid = UUID.randomUUID(),
    loginInfo = LoginInfo("facebook", "user@facebook.com"),
    firstName = None,
    lastName = None,
    fullName = None,
    email = None,
    avatarURL = None
  )

  /**
   * A Silhouette fake environment.
   */
  implicit val env: Environment[User, CookieAuthenticator] =
    new FakeEnvironment[User, CookieAuthenticator](Seq(identity.loginInfo -> identity))

  /**
   * The application.
   */
  lazy val application = new GuiceApplicationBuilder()
    .overrides(new FakeModule)
    .build()
}*/
