package chatContact

import com.greencatsoft.angularjs.core.Window
import com.greencatsoft.angularjs.{Factory, injectable}
import org.scalajs.dom.raw.WebSocket

@injectable("chatService")
class ChatService(window: Window) {
  def send(webSocket: WebSocket, message: String): Unit = webSocket.send(message)

  def findWebSocketUrl(): String = {
    val host = window.location.host
    val protocolLetter = window.location.protocol.last match {
      case 's' => "s"
      case _ => ""
    }
    s"ws$protocolLetter://$host/"
  }
}

@injectable("chatService")
class TaskServiceFactory(window: Window) extends Factory[ChatService] {
  override def apply() = new ChatService(window: Window)
}