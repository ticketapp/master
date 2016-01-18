package actors

import akka.actor.ActorSystem
import com.google.inject.Inject
import javax.inject.Singleton


@Singleton
class DuplicateTracksActorInstance @Inject() (system: ActorSystem) {
  val duplicateTracksActor = system.actorOf(DuplicateTracksActor.props, "DuplicateTracksActor")
}
