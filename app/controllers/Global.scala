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

  Akka.system.scheduler.schedule(initialDelay = 10.seconds, interval = 12.hours) {
    Logger.info("Scheduler.findEventsForPlaces started")
    scheduler.findEventsForPlaces()
  }

  Akka.system.scheduler.schedule(initialDelay = 20.minutes, interval = 12.hours) {
    Logger.info("Scheduler.findEventsForOrganizers started")
    scheduler.findEventsForOrganizers()
  }

  Akka.system.scheduler.schedule(initialDelay = 40.minutes, interval = 12.hours) {
    Logger.info("Scheduler.findTracksForArtists started")
    scheduler.findTracksForArtists()
  }
}