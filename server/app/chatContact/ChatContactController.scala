package chatContact

import play.api.libs.json.Json
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.{WebSocket, Controller}
import json.JsonHelper._
import play.api.Play.current

class ChatContactController extends Controller {
  def openSocket = WebSocket.acceptWithActor[String, String] { request => out =>
    ChatWebSocketActor.props(out)
  }

  implicit val clientActorRefAndMessageFormat = Json.format[ClientActorRefAndMessage]
  implicit val clientActorRefAndMessageFrameFormatter = FrameFormatter.jsonFrame[ClientActorRefAndMessage]

  def openAdminSocket = WebSocket.acceptWithActor[ClientActorRefAndMessage, ClientActorRefAndMessage] {
    implicit request => out => AdminChatWebSocketActor.props(out)
  }
}
