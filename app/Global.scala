import play.api._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import jobs.Scheduler

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    Akka.system.scheduler.schedule(2.minutes, 12.hours) {
      println("Scheduler started")
      Scheduler.start()
    }
    /*play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _ => startScheduler(app)
    }*/
  }
}