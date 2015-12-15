package application

import javax.inject.Inject

import com.google.inject.Singleton
import jobs.Scheduler
import org.joda.time.DateTime
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent._
import services.Utilities

import scala.concurrent.duration._


@Singleton
class Global @Inject()(val scheduler: Scheduler) extends Utilities {

  val hoursSinceMidnight: Int = DateTime.now().hourOfDay().get()

  val timeBefore4AMInHours = returnNumberOfHoursBetween4AMAndNow(hoursSinceMidnight).hours


  Akka.system.scheduler.schedule(initialDelay = timeBefore4AMInHours, interval = 12.hours) {
    Logger.info("Scheduler.findEventsForPlaces started")
    scheduler.findEventsForPlacesOneByOne()
  }

  Akka.system.scheduler.schedule(initialDelay = timeBefore4AMInHours + 50.minutes, interval = 12.hours) {
    Logger.info("Scheduler.findEventsForOrganizers started")
    scheduler.findEventsForOrganizersOneByOne()
  }

  Akka.system.scheduler.schedule(initialDelay = timeBefore4AMInHours + 75.minutes, interval = 12.hours) {
    Logger.info("Scheduler.findTracksForArtists started")
    scheduler.findTracksForArtistsOneByOne()
  }

  Akka.system.scheduler.schedule(initialDelay = timeBefore4AMInHours + 90.minutes, interval = 12.hours) {
    Logger.info("Scheduler.updateGeographicPointOfPlaces started")
    scheduler.updateGeographicPointOfPlaces50By50()
  }

  Akka.system.scheduler.schedule(initialDelay = timeBefore4AMInHours + 105.minutes, interval = 12.hours) {
    Logger.info("Scheduler.updateGeographicPointOfOrganizers started")
    scheduler.updateGeographicPointOfOrganizers50By50()
  }

  Akka.system.scheduler.schedule(initialDelay = timeBefore4AMInHours + 120.minutes, interval = 12.hours) {
    Logger.info("Scheduler.updateGeographicPointOfEvents started")
    scheduler.updateGeographicPointOfEvents50By50()
  }
}