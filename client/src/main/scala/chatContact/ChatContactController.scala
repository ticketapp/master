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
class ChatContactController(chatContactScope: ChatContactScope, httpService: HttpService, timeout: Timeout)
  extends AbstractController[ChatContactScope](chatContactScope) {

  val webSocket = new dom.WebSocket("ws://localhost:9000/chatContact")

  var areWeConnected: Boolean = false

  chatContactScope.messages = js.Array[Message]()

  webSocket.onmessage = (message: MessageEvent) => {

    timeout(() => chatContactScope.messages.push(Message(content = message.data.toString, fromClient = false)))
  }

  def send(message: String): Unit = {
    timeout(() => scope.messages.push(Message(content = message, fromClient = true)))
    webSocket.send(message)
  }
}
