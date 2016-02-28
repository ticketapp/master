package chatContact

import com.greencatsoft.angularjs.core.{HttpService, Timeout, Window}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import org.scalajs.dom
import org.scalajs.dom.MessageEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

case class Message(content: String, fromClient: Boolean)

@JSExportAll
@injectable("chatContactController")
class ChatContactController(chatContactScope: ChatContactScope, httpService: HttpService, timeout: Timeout,
                            window: Window, chatService: ChatService)
    extends AbstractController[ChatContactScope](chatContactScope) {

  chatContactScope.messages = js.Array[Message]()

  val host = window.location.host

  val webSocket = new dom.WebSocket(s"ws://$host/chat")

  var areWeConnected: Boolean = false

  webSocket.onmessage = (message: MessageEvent) =>
    timeout(() => chatContactScope.messages.push(Message(content = message.data.toString, fromClient = false)))

  def send(message: String) = {
    chatService.send(webSocket, message)
    timeout(() => chatContactScope.messages.push(Message(content = message, fromClient = true)))
  }
}
