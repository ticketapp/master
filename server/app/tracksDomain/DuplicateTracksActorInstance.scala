package tracksDomain

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.google.inject.Inject

@Singleton
class DuplicateTracksActorInstance @Inject() (system: ActorSystem) {
  val duplicateTracksActor = system.actorOf(DuplicateTracksActor.props, "DuplicateTracksActor")
}
