package services

//import json.JsonHelper.oAuth1InfoReads
//import json.OAuth1Info2
import _root_.java.util.Date
import anorm.SqlParser._
import anorm._
import com.fasterxml.jackson.databind.JsonNode
import controllers.DAOException
import play.api.libs.json.JsObject

//import json.JsonWriters
import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import play.api.{Logger, Application}
import play.libs.Json
import securesocial.core._
import securesocial.core.providers.Token

class InMemoryUserService(application: Application) extends UserServicePlugin(application) {

  def find(id: IdentityId): Option[Identity] = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("Find identity by IdentityId: %s".format(id))
    }
    val ret = DB.withConnection { implicit  connection =>
      SQL(s"SELECT ${USERS.FIELDS} FROM users_login WHERE userId={userId} AND providerId={providerId}").on(
        'userId -> id.userId,
        'providerId -> id.providerId
      ).as(USERS.parser *).headOption
    }
    Logger.debug("Found: " + ret)
    ret
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("Find identity by email and providerId: %s and %s".format(email, providerId))
    }
    DB.withConnection { implicit  connection =>
      SQL(s"SELECT ${USERS.FIELDS} FROM users_login WHERE email={email} AND providerId={providerId}").on(
        'email -> email,
        'providerId -> providerId
      ).as(USERS.parser *).headOption
    }
  }

  def save(user: Identity): Identity = {
    try {
      DB.withConnection { implicit connection =>
        SQL(s"INSERT INTO users_login(${USERS.FIELDS_LESS_ID}) VALUES (" +
          s"{userId}, {providerId}, {firstName}, {lastName}, {fullName}, " +
          s"{email}, {avatarUrl}, {authMethod}, " +
          s"{oAuth1Info}, {oAuth2Info}, {passwordInfo})").on(
            'userId -> user.identityId.userId,
            'providerId -> user.identityId.providerId,
            'firstName -> user.firstName,
            'lastName -> user.lastName,
            'fullName -> user.fullName,
            'email -> user.email,
            'avatarUrl -> user.avatarUrl,
            'authMethod -> user.authMethod.method,
            'oAuth1Info -> Json.stringify(Json.toJson(user.oAuth1Info)),
            'oAuth2Info -> Json.stringify(Json.toJson(user.oAuth2Info)),
            'passwordInfo -> Json.stringify(Json.toJson(user.passwordInfo))
          ).executeUpdate()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create user_login: " + e.getMessage)
    }
    user
  }

  def save(token: Token) {
    try {
      DB.withConnection { implicit connection =>
        SQL(s"INSERT INTO users_token(${TOKENS.FIELDS}) VALUES (" +
          s"{id}, {email}, {creationTime}, {expirationTime}, {isSignUp})").on(
            'id -> token.uuid,
            'email -> token.email,
            'creationTime -> token.creationTime.toDate,
            'expirationTime -> token.expirationTime.toDate,
            'isSignUp -> token.isSignUp
          ).executeUpdate()
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        throw new DAOException("Cannot create users_token: " + e.getClass + " " + e.getMessage + " " + e.getCause)
      }
    }
  }

  def findToken(uuid: String): Option[Token] = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("Find token by uuid: %s".format(uuid))
    }
    DB.withConnection { implicit  connection =>
      SQL(s"SELECT ${TOKENS.FIELDS} FROM users_token WHERE id={id}").on(
        'id -> uuid
      ).as(TOKENS.parser *).headOption
    }
  }

  def deleteToken(uuid: String) {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("Delete the token with uuid: %s".format(uuid))
    }
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users_token WHERE id={id}").on(
        'id -> uuid
      ).executeUpdate()
    }
  }

  def deleteTokens() {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("Delete all tokens")
    }
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users_token").executeUpdate()
    }
  }

  def deleteExpiredTokens() {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("Delete all expired tokens")
    }
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users_token WHERE expirationTime <= {now}").on(
        'now -> DateTime.now().toDate
      ).executeUpdate()
    }
  }
}

case class SSIdentity(
                       id: Option[Long],
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


object USERS {

  val FIELDS_LESS_ID = "userId, providerId, firstName, lastName, fullName, email, " +
    "avatarUrl, authMethod, oAuth1Info, oAuth2Info, passwordInfo"
  val FIELDS = "id, " + FIELDS_LESS_ID

  val parser = {
    get[Pk[Long]]("id") ~
      get[String]("userId") ~
      get[String]("providerId") ~
      get[String]("firstName") ~
      get[String]("lastName") ~
      get[String]("fullName") ~
      get[Option[String]]("email") ~
      get[Option[String]]("avatarUrl") ~
      get[String]("authMethod") ~
      get[Option[String]]("oAuth1Info") ~
      get[Option[String]]("oAuth2Info") ~
      get[Option[String]]("passwordInfo") map {
      case id~userId~providerId~firstName~lastName~fullName
        ~email~avatarUrl~authMethod~oAuth1Info~oAuth2Info
        ~passwordInfo => SSIdentity(id.toOption, IdentityId(userId, providerId),
        firstName, lastName, fullName, email, avatarUrl, AuthenticationMethod(authMethod),
        getOAuth1Info(oAuth1Info), None, None)
    }
  }

  def getOAuth1Info(value: Option[String]) : Option[OAuth1Info] = value match {
    case Some(o) => {
      Option.apply(Json.fromJson(Json.parse(o), classOf[OAuth1Info]))
    }
    case None => None
  }
}

object TOKENS {

  val FIELDS = "id, email, creationTime, expirationTime, isSignUp"

  val parser = {
    get[String]("id") ~
      get[String]("email") ~
      get[Date]("creationTime") ~
      get[Date]("expirationTime") ~
      get[Boolean]("isSignUp") map {
      case id~email~creationTime~expirationTime~isSignUp =>
        Token(id, email, new DateTime(creationTime), new DateTime(expirationTime), isSignUp)
    }
  }
}