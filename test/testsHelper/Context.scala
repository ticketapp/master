package testsHelper

import java.util.UUID

import application.User
import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import net.codingwell.scalaguice.ScalaModule
import org.specs2.specification.Scope
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.ExecutionContext.Implicits.global


trait Context extends Scope {

  class FakeModule extends AbstractModule with ScalaModule {
    def configure() = {
      bind[Environment[User, CookieAuthenticator]].toInstance(envvv)
    }
  }

  val identity = User(
    uuid = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"),
    loginInfo = LoginInfo("facebook", "user@facebook.com"),
    firstName = None,
    lastName = None,
    fullName = None,
    email = None,
    avatarURL = None
  )

  implicit val envvv: Environment[User, CookieAuthenticator] =
    new FakeEnvironment[User, CookieAuthenticator](Seq(identity.loginInfo -> identity))

  lazy val application = new GuiceApplicationBuilder().configure(Configuration.from(Map(
    "slick.dbs.default.driver" -> "slick.driver.PostgresDriver$",
    "slick.dbs.default.db.driver" -> "org.postgresql.Driver",
    "slick.dbs.default.db.url" -> "jdbc:postgresql://dbHost:5432/tests",
    "slick.dbs.default.db.user" -> "simon",
    "slick.dbs.default.db.password" -> "root",
    "slick.dbs.default.db.connectionTimeout" -> "5 seconds",
    "slick.dbs.default.db.connectionPool" -> "disabled")))
    .overrides(new FakeModule)
    .build()
}