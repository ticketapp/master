import akka.actor

import scala.concurrent.duration.DurationInt
import akka.actor.Props.apply
import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import akka.actor.Props
/*import actors.ReminderActor

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    val controllerPath = controllers.routes.Ping.ping.url
    play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _ => reminderDaemon(app)
    }

  }

  def reminderDaemon(app: Application) = {
    Logger.info("Scheduling the reminder daemon")
    val reminderActor = Akka.system(app).actorOf(Props(new ReminderActor()))
    Akka.system(app).scheduler.schedule(0 seconds, 5 minutes, reminderActor, "reminderDaemon")
  }

}*/