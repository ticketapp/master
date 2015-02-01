package services

import json.JsonHelper.oAuth1InfoReads
import _root_.java.util.Date
import anorm.SqlParser._
import anorm._
import com.fasterxml.jackson.databind.JsonNode
import controllers.DAOException
import play.libs.Json
import play.api.libs.json._
//import json.JsonWriters
import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import play.api.{Logger, Application}
import securesocial.core._
import securesocial.core.providers.Token
import json.JsonHelper

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
    /*println(user.oAuth1Info)
    println("user.oAuth1Info.toJson : " + Json.toJson(user.oAuth1Info))*/


    /*def writes(oAuth2Info: securesocial.core.OAuth2Info) = JsObject(Seq(
      "accessToken" -> JsString(oAuth2Info.accessToken),
      "tokenType" -> Json.toJson(oAuth2Info.tokenType),
      "expiresIn" -> Json.toJson(oAuth2Info.expiresIn),
      "refreshToken" -> Json.toJson(oAuth2Info.refreshToken)
    ))*/





    println("user.oAuth2Info : " + user.oAuth2Info)
    println("user.oAuth2Info<getOrElse : " + user.oAuth2Info.getOrElse(None))
    println("user.oAuth2Info.toJson : " + Json.toJson(user.oAuth2Info.getOrElse(None)))
    println("user.oAuth2Info.toJson.stringify : " + Json.stringify(Json.toJson(user.oAuth2Info)))



    /*println(user.passwordInfo)


    JsObject(
      "id" -> JsNumber(5) ::
        "username" -> JsString("sfsfsdf") ::
        "age" -> JsNumber(4) :: Nil
    )


    var test2 = user.oAuth2Info
    var test1 = user.oAuth1Info
    println(test2)
    println(test1)


    user.oAuth2Info match {
      case Some(o) => {
        val accessTokenVal = o.accessToken
        o.tokenType match {
          case Some(tokenTypeValue) =>
            val tokenTypeVal =  tokenTypeValue
          case None =>
        }
        o.expiresIn match {
          case Some(expiresInValue) =>
            val expiresInVal = expiresInValue
          case None => val expiresInVal = None
        }
        o.refreshToken match {
          case Some(refreshTokenValue) =>
            val refreshTokenVal = refreshTokenValue
          case None => val refreshTokenVal = None
        }
      }
      case None =>
    }

    var oAuth2InfoJson: JsValue = JsObject(Seq(
      "accessToken" -> JsString("sqdqsd")
    ))


    /*user.oAuth2Info match {
      case Some(o) => {
        val accessTokenVal = o.accessToken
        o.tokenType match {
          case Some(tokenTypeValue) =>
            val tokenTypeVal = tokenTypeValue
          case None => val tokenTypeVal = None
        }
        o.expiresIn match {
          case Some(expiresInValue) =>
            val expiresInVal = expiresInValue
          case None => val expiresInVal = None
        }
        o.refreshToken match {
          case Some(refreshTokenValue) =>
            val refreshTokenVal = refreshTokenValue
          case None => val refreshTokenVal = None
        }
      }
      case None =>
      /*JsObject(Seq(
        "accessToken" -> JsString(o.accessToken),
        "tokenType" -> o.tokenType,
        "expiresIn" -> o.expiresIn,
        "refreshToken" -> o.refreshToken
      ))*/
    }*/*/

    try {
      DB.withConnection { implicit connection =>
        SQL(s"""INSERT INTO users_login(${USERS.FIELDS_LESS_ID}) VALUES (
                {userId}, {providerId}, {firstName}, {lastName}, {fullName}, {email}, {avatarUrl}, {authMethod},
                {oAuth1Info}, {oAuth2Info}, {passwordInfo}
               )"""
        ).on(
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
        SQL(s"""INSERT INTO users_token(${TOKENS.FIELDS}) VALUES (
              {id}, {email}, {creationTime}, {expirationTime}, {isSignUp})"""
        ).on(
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
        None, None,None)
        //getOAuth1Info(oAuth1Info), getOAuth2Info(oAuth2Info), getPasswordInfo(passwordInfo))
    }
  }

  def getOAuth1Info(value: Option[String]) : Option[OAuth1Info] = value match {
    case Some(o) => {
      Option.apply(Json.fromJson(Json.parse(o), classOf[OAuth1Info]))
    }
    case None => None
  }

  def getOAuth2Info(value: Option[String]) : Option[OAuth2Info] = value match {
    case Some(o) => {
      Option.apply(Json.fromJson(Json.parse(o), classOf[OAuth2Info]))
    }
    case None => None
  }

  def getPasswordInfo(value: Option[String]) : Option[PasswordInfo] = value match {
    case Some(o) => {
      Option.apply(Json.fromJson(Json.parse(o), classOf[PasswordInfo]))
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