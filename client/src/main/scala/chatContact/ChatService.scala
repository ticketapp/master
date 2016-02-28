package chatContact

import com.greencatsoft.angularjs.{Factory, injectable}
import org.scalajs.dom.raw.WebSocket

@injectable("chatService")
class ChatService() {
  def send(webSocket: WebSocket, message: String): Unit = webSocket.send(message)
}

@injectable("chatService")
class TaskServiceFactory() extends Factory[ChatService] {
  override def apply() = new ChatService()
}