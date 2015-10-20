/*
import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import controllers.routes
import models.User
import net.codingwell.scalaguice.ScalaModule
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.{ApplicationLoader, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test._


class TestArtistController extends PlaySpecification with Mockito {
  sequential
//
//  val appBuilder = new GuiceApplicationBuilder()
//  val injector = appBuilder.injector()
//  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
//  val utilities = new Utilities
//  val trackMethods = new TrackMethods(dbConfProvider, utilities)
//  val genreMethods = new GenreMethods(dbConfProvider, utilities)
//  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
//  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
//  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
//    trackMethods, utilities)
//
//  val messagesApi =  MessagesApi
//
//  val injectorr = new GuiceApplicationBuilder() .load(
//    new play.api.inject.BuiltinModule,
//    bind[MessagesApi].to[MessagesApi]
//  ).injector
//
//  val artistController = new ArtistController(MessagesApi, utilities, dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack)
//

  "ArtistController" should {

    "be followed by a user" in new Context {
//      val eventuallyResult = controllers.ArtistController.findNearCity("abc", 1, 0)(FakeRequest())//RequestBuilder
//      status(eventuallyResult) mustBe 200
//      contentAsJson(eventuallyResult) mustBe Json.toJson(Seq.empty[Event])

//      val Some(result) = route(FakeRequest(GET, "/artists"))
//
//      status(result) mustEqual OK
//      contentType(result) mustEqual Some("application/json")
//      charset(result) mustEqual Some("utf-8")
//      println(contentAsJson(result))
//    }

//    "azadqsdqsd" in {
      val Some(result) = route(FakeRequest(routes.Application.index())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )

      status(result) must beEqualTo(OK)
    }
  }

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
      avatarURL = None)

    /**
     * A Silhouette fake environment.
     */
    implicit val env: Environment[User, CookieAuthenticator] = new FakeEnvironment[User, CookieAuthenticator](Seq(identity.loginInfo -> identity))

//    implicit val globalSettings:

    /**
     * The application.
     */
    lazy val application = new GuiceApplicationBuilder()
      .overrides(new FakeModule)
//      .loadConfig(ApplicationLoader.Context.mockitoSettings)
      .build()
  }
}
*/
import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{ Environment, LoginInfo }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import controllers.{ArtistController, routes}
import models.User
import net.codingwell.scalaguice.ScalaModule
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.Controller
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import controllers.ArtistController

class TestArtistController extends PlaySpecification with Mockito {
  sequential

  "The `index` action" should {
   /* "redirect to login page if user is unauthorized" in new Context {
      new WithApplication(application) {
        val Some(redirectResult) = route(FakeRequest(routes.Application.index())
          .withAuthenticator[CookieAuthenticator](LoginInfo("invalid", "invalid"))
        )

        status(redirectResult) must be equalTo SEE_OTHER

        val redirectURL = redirectLocation(redirectResult).getOrElse("")
        redirectURL must contain(routes.Application.signIn().toString)

        val Some(unauthorizedResult) = route(FakeRequest(GET, redirectURL))

        status(unauthorizedResult) must be equalTo OK
        contentType(unauthorizedResult) must beSome("text/html")
        contentAsString(unauthorizedResult) must contain("Silhouette - Sign In")
      }
    }*/
//
//    "return 200 if user is authorized" in new Context {
//      new WithApplication(application) {
//        val Some(result) = route(FakeRequest(routes.Application.index())
//          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
//        )
//
//        status(result) must beEqualTo(OK)
//      }
//    }

    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" in new Context {
      new WithApplication(application) {

        val Some(result) = route(FakeRequest(POST, "/artists/123/followByArtistId").withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(result) mustEqual NOT_FOUND
      }
    }
  }

  /**
   * The context.
   */
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
    implicit val env: Environment[User, CookieAuthenticator] = new FakeEnvironment[User, CookieAuthenticator](Seq(identity.loginInfo -> identity))

    /**
     * The application.
     */
    lazy val application = new GuiceApplicationBuilder()
      .overrides(new FakeModule)
      .build()
  }
}
