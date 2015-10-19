import javax.inject.Inject

import models.EventMethods
import play.api._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import jobs.Scheduler


class Global @Inject()(val scheduler: Scheduler) extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _ =>
        Akka.system.scheduler.schedule(120.seconds, 12.hours) {
          Logger.info("Scheduler started")
//          scheduler.start()
      }
    }
  }
}