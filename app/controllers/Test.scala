package controllers

import play.api.Logger
import play.api.mvc._
import akka.actor._
import javax.inject._

import actors.DuplicateTracksActor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.ask
/*
 follower counts SC
 regarder le next de facebook
 aller chercher les artistes d'une lettre et deux lettres
 enlever 0.1 par lettre en plus au temps de latence à partir de trois
 un seul match echonest : la prendre et lier à la page fb qi à le plus de followers (exemple ibeyi)
 hendrix : bug si getechoSongs recursif

 test implicit timeouts
 */


@Singleton
class Test @Inject()(system: ActorSystem) extends Controller {

  implicit val timeout: akka.util.Timeout = 5.seconds

//  val helloActor = system.actorOf(DuplicateTracksActor.props, "duplicateTracksActor")

  def sayHello(name: String) = Action.async {
    Future(Ok)
//    (helloActor ? SayHello(name)).mapTo[String].map { message =>
//      Logger.info(message)
//      Ok(message)
//    }
  }
}
//    "a" in {
//      val actorRef = TestActorRef[DuplicateTracksActor]
//      val future = (actorRef ? SayHello("msg")).mapTo[String]
//      val result = Await.result(future, actorTimeout.duration)
//
//      val trackId = UUID.randomUUID
//      val track = Track(trackId, "title100", "url", 's', "thumbnailUrl", "facebookUrl0", "artistName")
//
//
//
//      val future2 = (test ? FilterTracks(Set(track))).mapTo[Set[Track]]
//      val result2 = Await.result(future2, actorTimeout.duration)
//
//      result mustBe "Hello, msg"
//      result2 mustBe "processed the tick message"
//    }