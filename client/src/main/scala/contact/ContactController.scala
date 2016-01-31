package contact

import angularWebSocket.MessagesWebsocketFactory
import com.greencatsoft.angularjs.{AbstractController, injectable}
import org.scalajs.dom.console
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("contactController")
class ContactController(scope: ContactScope, webSocket: MessagesWebsocketFactory)
  extends AbstractController[ContactScope](scope) {
    scope.messages = webSocket.asInstanceOf[js.Array[Message]]
  console.log( webSocket)
}
