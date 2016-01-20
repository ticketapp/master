package contact

import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.{AbstractController, injectable}
import org.scalajs.dom
import org.scalajs.dom.MessageEvent

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("contactController")
class ContactController(scope: ContactScope, httpService: HttpService)
  extends AbstractController[ContactScope](scope) {

  val webSocket = new dom.WebSocket("ws://localhost:9000/chatContact")

  var receivedMessages = scala.collection.immutable.Seq.empty[String]

  webSocket.onmessage = (message: MessageEvent) => {

    receivedMessages = receivedMessages :+ message.data.toString
  }

  def send(message: String): Unit = webSocket.send(message)
}
