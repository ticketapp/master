package services

import _root_.java.util.Date
import anorm.SqlParser._
import anorm._
import com.fasterxml.jackson.databind.JsonNode
import controllers.DAOException
import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import play.api.{Logger, Application}
import securesocial.core._
import securesocial.core.providers.Token
import play.api.libs.json.JsNumber
import play.api.libs.json._
import play.api.libs.functional.syntax._
import json.JsonHelper._

import scala.language.postfixOps

class InMemoryUserService(application: Application) extends UserServicePlugin(application) {
/*

faire en sorte que ca find si luser est déjà enregistré au save


 */
  def find(id: IdentityId): Option[Identity] = {
    if (Logger.isDebugEnabled)
      Logger.debug(s"Find identity by IdentityId: $id")
    val ret = DB.withConnection { implicit  connection =>
      SQL(s"SELECT ${USERS.FIELDS} FROM users_login WHERE userId={userId} AND providerId={providerId}").on(
        'userId -> id.userId,
        'providerId -> id.providerId)
        .as(USERS.parser *)
        .headOption
    }
    Logger.debug("Found: " + ret)
    ret
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = {
    if (Logger.isDebugEnabled)
      Logger.debug(s"Find identity by email and providerId: $email and $providerId")
    DB.withConnection { implicit  connection =>
      SQL(
        s"""SELECT ${USERS.FIELDS} FROM users_login
           |  WHERE email = {email} AND providerId = {providerId}""".stripMargin)
        .on(
          'email -> email,
          'providerId -> providerId)
        .as(USERS.parser *).headOption
    }
  }

  def save(user: Identity): Identity = {
    def stringifyAuth1Info(oAuth1Info: Option[OAuth1Info]) = oAuth1Info match {
      case Some(oAuth) => Json.stringify(Json.toJson(oAuth))
      case _ => None
    }


    def stringifyAuth2Info(oAuth2Info: Option[OAuth2Info]) = oAuth2Info match {
      case Some(oAuth) => Json.stringify(Json.toJson(oAuth))
      case _ => None
    }


    def stringifyPasswordInfo(passwordInfo: Option[PasswordInfo]) = passwordInfo match {
      case Some(passwordInfoFound) => Json.stringify(Json.toJson(passwordInfoFound))
      case _ => None
    }

    try {
      //upsert
      DB.withConnection { implicit connection =>
        SQL(
          s"""INSERT INTO users_login(${USERS.FIELDS_LESS_ID})
             |VALUES ({userId}, {providerId}, {firstName}, {lastName}, {fullName}, {email}, {avatarUrl}, {authMethod},
             |{oAuth1Info}, {oAuth2Info}, {passwordInfo})""".stripMargin)
          .on(
            'userId -> user.identityId.userId,
            'providerId -> user.identityId.providerId,
            'firstName -> user.firstName,
            'lastName -> user.lastName,
            'fullName -> user.fullName,
            'email -> user.email,
            'avatarUrl -> user.avatarUrl,
            'authMethod -> user.authMethod.method,
            'oAuth1Info -> stringifyAuth1Info(user.oAuth1Info),
            'oAuth2Info -> stringifyAuth2Info(user.oAuth2Info),
            'passwordInfo -> stringifyPasswordInfo(user.passwordInfo))
          .executeUpdate()
      }
    } catch {
      case e: Exception => println("Cannot create user_login: " + e.getMessage)
    }
    user
  }

  def save(token: Token) = try {
    DB.withConnection { implicit connection =>
      SQL(s"""INSERT INTO users_token(${TOKENS.FIELDS}) VALUES (
            {id}, {email}, {creationTime}, {expirationTime}, {isSignUp})""")
        .on(
          'id -> token.uuid,
          'email -> token.email,
          'creationTime -> token.creationTime.toDate,
          'expirationTime -> token.expirationTime.toDate,
          'isSignUp -> token.isSignUp)
        .executeUpdate()
    }
  } catch {
    case e: Exception =>
      e.printStackTrace()
      throw new DAOException("Cannot create users_token: " + e.getClass + " " + e.getMessage + " " + e.getCause)
  }


  def findToken(uuid: String): Option[Token] = {
    if (Logger.isDebugEnabled)
      Logger.debug(s"Find token by uuid: $uuid")
    DB.withConnection { implicit  connection =>
      SQL(s"SELECT ${TOKENS.FIELDS} FROM users_token WHERE id = {id}")
        .on('id -> uuid)
        .as(TOKENS.parser *)
        .headOption
    }
  }

  def deleteToken(uuid: String): Unit = {
    if (Logger.isDebugEnabled)
      Logger.debug(s"Delete the token with uuid: $uuid")
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users_token WHERE id={id}")
        .on('id -> uuid)
        .executeUpdate()
    }
  }

  def deleteTokens(): Unit = {
    if (Logger.isDebugEnabled)
      Logger.debug("Delete all tokens")
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users_token").executeUpdate()
    }
  }

  def deleteExpiredTokens(): Unit = {
    if (Logger.isDebugEnabled)
      Logger.debug("Delete all expired tokens")
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users_token WHERE expirationTime <= {now}")
        .on('now -> DateTime.now().toDate)
        .executeUpdate()
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

  implicit val oAuth2InfoReads = Json.reads[OAuth2Info]

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
      case id ~ userId ~ providerId ~ firstName ~ lastName ~ fullName
        ~ email ~ avatarUrl ~ authMethod ~ oAuth1Info ~ oAuth2Info
        ~ passwordInfo => SSIdentity(id.toOption, IdentityId(userId, providerId),
        firstName, lastName, fullName, email, avatarUrl, AuthenticationMethod(authMethod),
        returnOAuth1Info(oAuth1Info), returnOAuth2Info(oAuth2Info), returnPasswordInfo(passwordInfo))
    }
  }

  //ecrire une fonction pour les trois suivantes avec match type trois case et un default error
  def returnOAuth1Info(oAuth1InfoOptionString: Option[String]): Option[OAuth1Info] = oAuth1InfoOptionString match {
    case Some(value) => Json.fromJson[OAuth1Info](Json.parse(value)) match {
      case JsSuccess(oAuth1Info, _) => Some(oAuth1Info)
      case JsError(errors) => println("Errors in returnOAuth1Info : " + errors.mkString); None
    }
    case None => None
  }

  def returnOAuth2Info(oAuth2InfoOptionString: Option[String]): Option[OAuth2Info] = oAuth2InfoOptionString match {
    case Some(value) => Json.fromJson[OAuth2Info](Json.parse(value)) match {
      case JsSuccess(oAuth2Info, _) => Some(oAuth2Info)
      case JsError(errors) => println("Errors in returnOAuth2Info : " + errors.mkString); None
    }
    case None => None
  }

  def returnPasswordInfo(passwordInfoString: Option[String]): Option[PasswordInfo] = passwordInfoString match {
    case Some(value) => Json.fromJson[PasswordInfo](Json.parse(value)) match {
      case JsSuccess(passwordInfo, _) => Some(passwordInfo)
      case JsError(errors) => println("Errors in returnPasswordInfo : " + errors.mkString); None
    }
    case None => None
  }
}

object TOKENS {
val FIELDS = "id, email, creationTime, expirationTime, isSignUp"

val parser =
  get[String]("id") ~
    get[String]("email") ~
    get[Date]("creationTime") ~
    get[Date]("expirationTime") ~
    get[Boolean]("isSignUp") map {
    case id ~ email ~ creationTime ~ expirationTime ~ isSignUp =>
      Token(id, email, new DateTime(creationTime), new DateTime(expirationTime), isSignUp)
  }
}