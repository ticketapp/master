package userDomain

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import javax.imageio.ImageIO
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.{Play, Logger}
import play.api.Play.current
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsSuccess, JsError, Json}
import play.api.libs.ws.{WS, WSClient}
import play.api.mvc._
import services.LoggerHelper
import json.JsonHelper._

import scala.concurrent.Future
import scala.util.control.NonFatal


class UserController @Inject() (ws: WSClient,
                                val messagesApi: MessagesApi,
                                val env: Environment[User, CookieAuthenticator],
                                val userMethods: UserMethods)
  extends Silhouette[User, CookieAuthenticator] with LoggerHelper {

  def getTracksRemoved = SecuredAction.async { implicit request =>
    userMethods.findUUIDOfTracksRemoved(request.identity.uuid) map { response =>
      Ok(Json.toJson(response))
    } recover {
      case e =>
        Logger.error("UserController.getTracksRemoved: ", e)
        InternalServerError
    }
  }

  def isConnected = SecuredAction { implicit request =>
      Ok(Json.toJson(true))
  }

  case class Token(token: String)
  def findFacebookAccessToken = SecuredAction { implicit request =>
    val token = Json.parse("""{"token": 1}""")
      Ok(Json.toJson(token))
  }

  def getUserGeographicPoint = Action.async { implicit request =>
    WS.url("http://ip-api.com/json/" + request.remoteAddress)
      .get()
      .map { response =>
        Ok(Json.toJson(response.json))
      } recover {
      case e =>
        Logger.error("UserController.getUserGeographicPoint: ", e)
        InternalServerError
    }
  }

  def createRib = SecuredAction.async { implicit request =>
    val validatedUserRib = request.body.asJson.get.validate[FromClientRib]

    validatedUserRib match {
      case error: JsError =>
        log(error.toString)
        Future(InternalServerError(error.toString))
      case userRib: JsSuccess[FromClientRib] =>
        val rib = userMethods.fromClientRibToRib(userRib.get, request.identity.uuid)
        userMethods.createRib(rib) map { response =>
          Ok(Json.toJson(response))
        } recover { case NonFatal(e) =>
          log(e.getMessage)
          InternalServerError(e.getMessage)
        }
    }
  }

  def updateRib = SecuredAction.async { implicit request =>
    val validatedUserRib = request.body.asJson.get.validate[FromClientRib]

    validatedUserRib match {
      case error: JsError =>
        log(error.toString)
        Future(InternalServerError(error.toString))
      case userRib: JsSuccess[FromClientRib] =>
        val rib = userMethods.fromClientRibToRib(userRib.get, request.identity.uuid)
        userMethods.updateRib(rib) map { response =>
          Ok(Json.toJson(response))
        } recover { case NonFatal(e) =>
          log(e.getMessage)
          InternalServerError(e.getMessage)
        }
    }
  }

  def findRibsByUserId(userId: String) = SecuredAction(Administrator()).async { implicit request =>
    val uuid = UUID.fromString(userId)
    userMethods.findRibsByUserId(uuid) map { ribs =>
      Ok(Json.toJson(ribs))
    }
  }

  def findUsersRibs = SecuredAction.async { implicit request =>
    val uuid = request.identity.uuid
    userMethods.findRibsByUserId(uuid) map { ribs =>
      Ok(Json.toJson(ribs))
    }
  }

  def createIdCard = SecuredAction.async(parse.multipartFormData) { request =>
    val userId = request.identity.uuid

    request.body.file("picture").map { image =>
      image.contentType match {
        case Some(fileExtension) if fileExtension == "jpg" =>
          val filename = UUID.randomUUID()
          image.ref.moveTo(new File(Play.application.path.getPath + "/idCards/" + filename), replace = true)
          userMethods.createIdCard(IdCard(filename, userId)) map { response =>
            Ok(Json.toJson(response))
          }
        case _ =>
          Future(Unauthorized("Wrong content type"))
      }
    }.getOrElse {
      Future(BadRequest)
    }
  }

  def findIdCardImageForUser(uuid: String) = SecuredAction.async { request =>
    val userUuid = request.identity.uuid
    userMethods.findIdCardsByUserId(userUuid) map { idCards =>
      idCards.find(_.uuid == uuid) match {
        case Some(idCard) =>
          getImageForUUID(uuid)
        case _ =>
          log("error found id card for user")
          InternalServerError("userController.findIdCardImageForUser")
      }
    }
  }

  def findIdCardImages(uuid: String) = SecuredAction(Administrator()) { request =>
    getImageForUUID(uuid)
  }

  def getImageForUUID(uuid: String): Result = {
    val imageFile = new File(Play.application.path.getPath + "/idCards/" + uuid)
    val image = ImageIO.read(imageFile)
    if (imageFile.length > 0) {
      val baos = new ByteArrayOutputStream()
      ImageIO.write(image, "jpg", baos)
      Ok(baos.toByteArray).as("image/jpg")
    } else
      log("error found image of id card for user")
    InternalServerError("userController.findIdCardImageForUser")
  }

  def findIdCardsByUserId(userId: String) = SecuredAction(Administrator()).async { request =>
    val userUuid = UUID.fromString(userId)
    userMethods.findIdCardsByUserId(userUuid) map { idCards =>
      Ok(Json.toJson(idCards))
    }
  }

  def findUsersIdCards = SecuredAction.async { request =>
    val userUuid = request.identity.uuid
    userMethods.findIdCardsByUserId(userUuid) map { idCards =>
      Ok(Json.toJson(idCards))
    }
  }
}
