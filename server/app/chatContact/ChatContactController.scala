package chatContact

import javax.inject.Inject
import play.api.Play.current
import akka.actor.{Props, ActorRef, Actor}
import play.api.mvc.{WebSocket, Controller}


case class ChatContactMessage(content: String)

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
    case _ =>
      out ! "I did not received your message: "
  }
}

class MessagesController @Inject() extends Controller {

  var messages = Seq(ChatContactMessage(content = "hello"))

  def openSocket = WebSocket.acceptWithActor[String, String] { request => out =>
    MyWebSocketActor.props(out)
  }
}
