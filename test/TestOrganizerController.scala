import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import net.codingwell.scalaguice.ScalaModule
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import org.specs2.matcher.Matchers
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.test.{WithServer, FakeRequest, PlaySpecification, WithApplication}
import services.{SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}
import silhouette.UserDAOImpl
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent.Await
import scala.concurrent.duration._

class TestOrganizerController extends PlaySpecification with Mockito {
  sequential

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities()
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val geographicPointMethods = new SearchGeographicPoint(dbConfProvider, utilities)
  val tariffMethods = new TariffMethods(dbConfProvider, utilities)
  val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  val addressMethods = new AddressMethods(dbConfProvider, utilities, geographicPointMethods)
  val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, addressMethods, utilities, geographicPointMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  val eventMethods = new EventMethods(dbConfProvider, organizerMethods, placeMethods, artistMethods, tariffMethods,
    geographicPointMethods, utilities)
  val userDAOImpl = new UserDAOImpl(dbConfProvider)

  "organizer controller" should {

    "create an organizer" in new Context {
      new WithApplication(application) {
        val organizer = Organizer(None, None, "orgaTest")

        val Some(result) = route(FakeRequest(POST, "/organizers/create")
        .withJsonBody(Json.parse("""{ "facebookId": 111, "name": "test" }"""))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(result) mustEqual OK
      }
    }

    "find a list of organizers" in new Context {
      new WithApplication(application) {
        val Some(organizers) = route(FakeRequest(GET, "/organizers?numberToReturn=" + 10 + "&offset=" + 0))
        contentAsJson(organizers).toString() must contain(""""facebookId":"111","name":"test"""")
      }
    }

    "find a list of organizer by containing" in new Context {
      new WithApplication(application) {
        val Some(organizers) = route(FakeRequest(GET, "/organizers/containing/test"))
        contentAsJson(organizers).toString() must contain(""""facebookId":"111","name":"test"""")
      }
    }

    "find one organizer by id" in new Context {
      new WithApplication(application) {
        val organizerId = await(organizerMethods.findAllContaining("test")).headOption.get.organizer.id
        val Some(organizer) = route(FakeRequest(GET, "/organizers/" + organizerId.get))
        contentAsJson(organizer).toString() must contain(""""facebookId":"111","name":"test"""")
      }
    }

    "follow and unfollow an organizer by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val organizerId = await(organizerMethods.findAllContaining("test")).head.organizer.id
        val Some(response) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/followByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/unfollowOrganizerByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response1) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "return an error if an user try to follow an organizer twice" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val organizerId = await(organizerMethods.findAllContaining("test")).head.organizer.id
        val Some(response) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/followByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/followByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response1) mustEqual CONFLICT

        val Some(response2) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/unfollowOrganizerByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "follow an unfollow an organizer by facebookId" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val organizerId = await(organizerMethods.findAllContaining("test")).head.organizer.id
        val Some(response) = route(FakeRequest(POST, "/organizers/111/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(response2) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/unfollowOrganizerByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find followed organizers" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val organizerId = await(organizerMethods.findAllContaining("test")).head.organizer.id
        val Some(response) = route(FakeRequest(POST, "/organizers/111/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(organizers) = route(FakeRequest(GET, "/organizers/followed/")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(organizers).toString() must contain(""""facebookId":"111","name":"test"""")

        val Some(response2) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/unfollowOrganizerByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find one followed organizer by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val organizerId = await(organizerMethods.findAllContaining("test")).head.organizer.id
        val Some(response) = route(FakeRequest(POST, "/organizers/111/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(organizers) = route(FakeRequest(GET, "/organizers/" + organizerId.get + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(organizers) mustEqual Json.parse("true")

        val Some(response2) = route(FakeRequest(POST, "/organizers/" + organizerId.get + "/unfollowOrganizerByOrganizerId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    /*"find organizers near city" {

    }*/
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
}