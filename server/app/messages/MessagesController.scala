package messages

import javax.inject.Inject
import akka.actor._
import play.api.Play.current
import play.api.mvc.{Controller, WebSocket}

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
    case _ =>
      out ! "I not received your message: "
  }
}

class MessagesController @Inject() extends Controller {

  var messages = Seq(Message(content = "hello"))

  def openSocket = WebSocket.acceptWithActor[String, String] { request => out =>
    MyWebSocketActor.props(out)
  }
}