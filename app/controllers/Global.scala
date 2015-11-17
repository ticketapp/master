package controllers

import javax.inject.Inject

import com.google.inject.Singleton
import jobs.Scheduler
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent._

import scala.concurrent.duration._

@Singleton
class Global @Inject()(val scheduler: Scheduler) {

  Akka.system.scheduler.schedule(initialDelay = 10.hours, interval = 12.hours) {
    Logger.info("Scheduler.findEventsForPlaces started")
    scheduler.findEventsForPlaces()
  }

  Akka.system.scheduler.schedule(initialDelay = 20.hours, interval = 12.hours) {
    Logger.info("Scheduler.findEventsForOrganizers started")
    scheduler.findEventsForOrganizers()
  }

  Akka.system.scheduler.schedule(initialDelay = 40.hours, interval = 12.hours) {
    Logger.info("Scheduler.findTracksForArtists started")
    scheduler.findTracksForArtists()
  }

  Akka.system.scheduler.schedule(initialDelay = 80.hours, interval = 12.hours) {
    Logger.info("Scheduler.updateGeographicPoints started")
    scheduler.updateGeographicPoints()
  }
}