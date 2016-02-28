package chatContact

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, SubscribeAck}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import chatContact.ClientOrAdmin.ClientOrAdmin
import play.api.Logger

import scala.concurrent.duration._
import scala.util.Success

object ClientOrAdmin extends Enumeration {
  type ClientOrAdmin = Value
  val Client, Admin = Value
}

case class ClientActorRefAndMessage(actorRef: String, msg: String, to: ClientOrAdmin)
case class AdminMessage(msg: String)

object ChatWebSocketActor {
  def props(out: ActorRef): Props = Props(new ChatWebSocketActor(out))
}

object AdminChatWebSocketActor {
  def props(out: ActorRef): Props = Props(new AdminChatWebSocketActor(out))
}

class ChatWebSocketActor(out: ActorRef) extends Actor {

  implicit val timeout: akka.util.Timeout = 5.seconds

  override def receive = {
    case adminMessage: AdminMessage =>
      out ! adminMessage.msg

    case msg =>
      val msgToAdmins = ClientActorRefAndMessage(actorRef = self.toString, msg = msg.toString, to = ClientOrAdmin.Admin)
      val mediator = DistributedPubSub(context.system).mediator
      mediator ! Publish("admin", msgToAdmins)
  }
}

class AdminChatWebSocketActor(out: ActorRef) extends Actor {
  implicit val timeout: akka.util.Timeout = 5.seconds
  import DistributedPubSubMediator.Subscribe
  import akka.cluster.pubsub.DistributedPubSub

  import scala.concurrent.ExecutionContext.Implicits.global

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("admin", self)

  override def receive = {
    case subscribe: SubscribeAck =>
      Logger.info("An admin has connected to the chat.")

    case fromClient: ClientActorRefAndMessage if fromClient.to == ClientOrAdmin.Admin =>
      out ! fromClient

    case fromAdmin: ClientActorRefAndMessage if fromAdmin.to == ClientOrAdmin.Client =>
      context.actorSelection(fromAdmin.actorRef).resolveOne().onComplete {
        case Success(clientActorRef) =>
          clientActorRef ! AdminMessage(fromAdmin.msg)

          val adminsBroadcastMsg = ClientActorRefAndMessage(
            actorRef = fromAdmin.actorRef,
            msg = fromAdmin.msg,
            to = ClientOrAdmin.Admin)
          mediator ! Publish("admin", adminsBroadcastMsg)

        case _ =>
          out ! "This clientActorRef doesn't exist."
      }

    case _ =>
      Logger.info("Unhandled message")
  }
}