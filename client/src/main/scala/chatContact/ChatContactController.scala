package chatContact

import com.greencatsoft.angularjs.core.{HttpService, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import org.scalajs.dom
import org.scalajs.dom.MessageEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

case class Message(content: String, fromClient: Boolean)

@JSExportAll
@injectable("chatContactController")
class ChatContactController(chatContactScope: ChatContactScope, httpService: HttpService, timeout: Timeout,
                            chatService: ChatService)
    extends AbstractController[ChatContactScope](chatContactScope) {

  chatContactScope.messages = js.Array[Message]()

  var areWeConnected: Boolean = false

  val webSocket = new dom.WebSocket(chatService.findWebSocketUrl() + "chat")

  webSocket.onmessage = (message: MessageEvent) =>
    timeout(() => chatContactScope.messages.push(Message(content = message.data.toString, fromClient = false)))

  def send(message: String) = {
    chatService.send(webSocket, message)
    timeout(() => chatContactScope.messages.push(Message(content = message, fromClient = true)))
  }
}
