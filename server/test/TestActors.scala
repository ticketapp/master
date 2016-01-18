import actors.DuplicateTracksActor
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask
import testsHelper.GlobalApplicationForModels
import scala.concurrent.Await

//import akka.testkit.{ TestActors, DefaultTimeout, ImplicitSender, TestKit, TestActorRef}

//object TestActorss {
//  class EchoActor extends Actor {
//    def receive = {
//      case x ⇒ sender ! x
//    }
//  }
//}

class TestActors/*(_system: ActorSystem)*/ extends GlobalApplicationForModels {

//  def this() = this(ActorSystem("TestActors"))

//  import TestActorss._

//  override def afterAll {
//    system.shutdown()
//  }

  "An Echo actor" must {

    "send back messages unchanged" in {


//      val actorRef = TestActorRef[DuplicateTracksActor]
//
//      val future = (actorRef ? SayHello("msg")).mapTo[String]
//      val result = Await.result(future, actorTimeout.duration)
//
//      result mustBe "processed the tick message"
    }
  }
}