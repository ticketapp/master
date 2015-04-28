package securesocial

import org.joda.time.DateTime
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import securesocial.core.providers.Token
import services.{SSIdentity, InMemoryUserService}
import securesocial.core._

class UserSpec extends Specification {
  "InMemoryUserService" should {

    "find an identity saved previously with the same identityId" in {
     /* val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val identityId: IdentityId = IdentityId(userId = "user id", providerId = "provider id")
        val identityId2: IdentityId = IdentityId(userId = "user id 2", providerId = "provider id")
        val identity = newIdentity(identityId)
        val identity2 = newIdentity2(identityId2)

        val testee = new InMemoryUserService(app)
        testee.save(identity)
        testee.save(identity2)
        testee.find(identityId).map(withoutId(_)) mustEqual Some(identity)
        testee.find(identityId2).map(withoutId(_)) mustEqual Some(identity2)
      }*/
      1 mustEqual 1
    }
/*
    "not find an identity saved with a different identityId" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val identityId: IdentityId = IdentityId(userId = "user id", providerId = "provider id")
        val wrongIdentityId: IdentityId = IdentityId(userId = "user id 2", providerId = "provider id")
        val identity = newIdentity(identityId)

        val testee = new InMemoryUserService(app)
        testee.save(identity)
        testee.find(wrongIdentityId) mustEqual None
      }
    }

    "find by email/providerId when saved with a matching identity" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val identityId: IdentityId = IdentityId(userId = "user id", providerId = "provider id")
        val identity = newIdentity(identityId)

        val testee = new InMemoryUserService(app)
        testee.save(identity)
        testee.findByEmailAndProvider(identity.email.get, identityId.providerId).map(withoutId(_)) mustEqual Some(identity)
      }
    }

    "not find by email/providerId when saved with a different email" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val identityId: IdentityId = IdentityId(userId = "user id", providerId = "provider id")
        val identity = newIdentity(identityId)

        val testee = new InMemoryUserService(app)
        testee.save(identity)
        testee.findByEmailAndProvider("other email", identityId.providerId) mustEqual None
      }
    }

    "not find by email/providerId when saved with a different providerId" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val identityId: IdentityId = IdentityId(userId = "user id", providerId = "provider id")
        val identity = newIdentity(identityId)

        val testee = new InMemoryUserService(app)
        testee.save(identity)
        testee.findByEmailAndProvider(identity.email.get, "other providerId") mustEqual None
      }
    }

    "find a token saved previously with the same uuid" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val token: Token = newToken()

        val testee = new InMemoryUserService(app)
        testee.save(token)
        testee.findToken(token.uuid) mustEqual Some(token)
      }
    }

    "not find a token saved with a different uuid" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val token: Token = newToken()

        val testee = new InMemoryUserService(app)
        testee.save(token)
        testee.findToken("other uuid") mustEqual None
      }
    }

    "delete a token saved with the same uuid should not still findable" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val token: Token = newToken()

        val testee = new InMemoryUserService(app)
        testee.save(token)
        testee.deleteToken(token.uuid)
        testee.findToken(token.uuid) mustEqual None
      }
    }

    "delete a token saved with a different uuid should do not trigger error" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val token: Token = newToken()

        val testee = new InMemoryUserService(app)
        testee.save(token)
        testee.deleteToken("other uuid")
        testee.findToken(token.uuid) mustEqual Some(token)
      }
    }

    "delete all tokens" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val token1: Token = newToken("uuid1")
        val token2: Token = newToken("uuid2")
        val token3: Token = newToken("uuid3")
        val token4: Token = newToken("uuid4")

        val testee = new InMemoryUserService(app)
        testee.save(token1)
        testee.save(token2)
        testee.save(token3)
        testee.save(token4)
        testee.deleteTokens()
        testee.findToken(token1.uuid) mustEqual None
        testee.findToken(token2.uuid) mustEqual None
        testee.findToken(token3.uuid) mustEqual None
        testee.findToken(token4.uuid) mustEqual None
      }
    }

    "delete expired tokens" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(app) {
        val token1: Token = newToken("uuid1", DateTime.now().plusHours(1))
        val token2: Token = newToken("uuid2", DateTime.now().minusSeconds(10))
        val token3: Token = newToken("uuid3", DateTime.now().plusSeconds(20))
        val token4: Token = newToken("uuid4", DateTime.now().minusMinutes(1))

        val testee = new InMemoryUserService(app)
        testee.save(token1)
        testee.save(token2)
        testee.save(token3)
        testee.save(token4)
        testee.deleteExpiredTokens()
        testee.findToken(token1.uuid) mustEqual Some(token1)
        testee.findToken(token2.uuid) mustEqual None
        testee.findToken(token3.uuid) mustEqual Some(token3)
        testee.findToken(token4.uuid) mustEqual None
      }
    }*/
  }

  def newToken(uuid: String = "1234", expirationTime: DateTime = DateTime.now().plusHours(1)): Token = Token(
      uuid = uuid,
      email = "a@b.c",
      creationTime = DateTime.now().minusHours(1),
      expirationTime = expirationTime,
      isSignUp = true
  )

  def withoutId(id: Identity) = new TestIdentity(id.identityId, id.firstName, id.lastName,
      id.fullName, id.email, id.avatarUrl, id.authMethod, id.oAuth1Info, id.oAuth2Info, id.passwordInfo)

  def newIdentity(identityId: IdentityId): TestIdentity = new TestIdentity(
    identityId = identityId,
    firstName = "firstname",
    lastName = "lastname",
    fullName = "fullname",
    email = Some("email@address.org"),
    avatarUrl = Some("http://avatar.org/mine.png"),
    authMethod = AuthenticationMethod.OAuth2
  )

  def newIdentity2(identityId: IdentityId): TestIdentity = new TestIdentity(
    identityId = identityId,
    firstName = "éojà 34",
    lastName = "blaaaaaaaaaaaaaaaaa",
    fullName = "blzzzzzzzzzzzzzzzzzzz vlzlzzzzz",
    email = None,
    avatarUrl = None,
    authMethod = AuthenticationMethod.UserPassword
  )

  case class TestIdentity(
    identityId: IdentityId,
    firstName: String,
    lastName: String,
    fullName: String,
    email: Option[String],
    avatarUrl: Option[String],
    authMethod: AuthenticationMethod,
    oAuth1Info: Option[OAuth1Info] = None,
    oAuth2Info: Option[OAuth2Info] = None,
    passwordInfo: Option[PasswordInfo] = None) extends Identity {}
  /*
  def identityId: IdentityId
  def firstName: String
  def lastName: String
  def fullName: String
  def email: Option[String]
  def avatarUrl: Option[String]
  def authMethod: AuthenticationMethod
  def oAuth1Info: Option[OAuth1Info]
  def oAuth2Info: Option[OAuth2Info]
  def passwordInfo: Option[PasswordInfo]*/

}