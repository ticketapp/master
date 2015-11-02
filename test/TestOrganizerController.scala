import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import net.codingwell.scalaguice.ScalaModule
import org.specs2.mock.Mockito
import org.specs2.specification.{AfterAll, BeforeAll, Scope}
import play.api.db.evolutions.Evolutions
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test._
import play.api.{Configuration, Play}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps


class TestOrganizerController extends GlobalApplicationForControllers {
    sequential

  "organizer controller" should {

    "create an organizer" in {
      val Some(result) = route(FakeRequest(POST, "/organizers/create")
          .withJsonBody(Json.parse("""{ "facebookId": 111, "name": "test" }"""))
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(result) mustEqual Json.parse("""{"organizer":{"id":3,"facebookId":"111","name":"test","verified":false}}""")

      status(result) mustEqual OK
    }

    "find a list of organizers" in {
       val Some(organizers) = route(FakeRequest(GET, "/organizers?numberToReturn=" + 10 + "&offset=" + 0))

       contentAsJson(organizers).toString() must contain(""""name":"name0"""")
    }

    "find one organizer by id" in {
      val Some(organizer) = route(FakeRequest(GET, "/organizers/" + 1))

      contentAsJson(organizer).toString() must contain(""""name":"name0"""")
    }

    "find a list of organizer containing" in {
      val Some(organizers) = route(FakeRequest(GET, "/organizers/containing/name0"))

      contentAsJson(organizers).toString() must contain(""""name":"name0"""")
    }

    "follow and unfollow an organizer by id" in {
      val Some(response) = route(FakeRequest(POST, "/organizers/" + 1 + "/followByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response1) = route(FakeRequest(POST, "/organizers/" + 1 + "/unfollowOrganizerByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow an organizer twice" in {
      val Some(response) = route(FakeRequest(POST, "/organizers/" + 1 + "/followByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response1) = route(FakeRequest(POST, "/organizers/" + 1 + "/followByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response2) = route(FakeRequest(POST, "/organizers/" + 1 + "/unfollowOrganizerByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual CONFLICT

      status(response2) mustEqual OK
    }

    "follow an organizer by facebookId" in {
      val Some(response) = route(FakeRequest(POST, "/organizers/" + "facebookId" + "/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED
    }

    "find followed organizers" in {
      val Some(organizers) = route(FakeRequest(GET, "/organizers/followed/")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(organizers).toString() must contain(""""facebookId":"facebookId","name":"name1"""")
    }

    "find one followed organizer by id" in {
      val Some(organizers) = route(FakeRequest(GET, "/organizers/" + 2 + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(organizers) mustEqual OK

      contentAsJson(organizers) mustEqual Json.parse("true")
    }

    /*"find organizers near city"*/
  }
}

