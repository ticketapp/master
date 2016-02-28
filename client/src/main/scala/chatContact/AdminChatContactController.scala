package chatContact

import com.greencatsoft.angularjs.core.{HttpService, Timeout, Window}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import org.scalajs.dom
import org.scalajs.dom.MessageEvent
import org.scalajs.dom.raw.{CloseEvent, ErrorEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("adminChatContactController")
class AdminChatContactController(chatContactScope: AdminChatContactScope, httpService: HttpService, timeout: Timeout,
                                 chatService: ChatService)
    extends AbstractController[AdminChatContactScope](chatContactScope) {

  chatContactScope.messages = js.Array[Message]()

  val webSocket = new dom.WebSocket(chatService.findWebSocketUrl() + "chat")

  var areWeConnected: Boolean = false

  webSocket.onclose = (closeEvent: CloseEvent) => println(closeEvent.reason)
  webSocket.onerror = (errorEvent: ErrorEvent) => println(errorEvent.message)

  webSocket.onmessage = (message: MessageEvent) =>
    timeout(() => chatContactScope.messages.push(Message(content = message.data.toString, fromClient = true)))

  def send(message: String) = {
    chatService.send(webSocket, message)
    timeout(() => chatContactScope.messages.push(Message(content = message, fromClient = false)))
  }
}
