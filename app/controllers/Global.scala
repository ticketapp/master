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

//  Akka.system.scheduler.schedule(initialDelay = 200000.seconds, interval = 12.hours) {
//    Logger.info("Scheduler.findEventsForPlaces started")
//    scheduler.findEventsForPlacesOneByOne()
//  }
//
//  Akka.system.scheduler.schedule(initialDelay = 120.seconds, interval = 12.hours) {
//    Logger.info("Scheduler.findEventsForOrganizers started")
//    scheduler.findEventsForOrganizersOneByOne()
//  }
//
//  Akka.system.scheduler.schedule(initialDelay = 60000.seconds, interval = 12.hours) {
//    Logger.info("Scheduler.findTracksForArtists started")
//    scheduler.findTracksForArtistsOneByOne()
//  }
//
//  Akka.system.scheduler.schedule(initialDelay = 60.second, interval = 12.hours) {
//    Logger.info("Scheduler.updateGeographicPointOfPlaces started")
//    scheduler.updateGeographicPointOfPlaces50By50()
//  }
//
//  Akka.system.scheduler.schedule(initialDelay = 240.seconds, interval = 12.hours) {
//    Logger.info("Scheduler.updateGeographicPointOfOrganizers started")
//    scheduler.updateGeographicPointOfOrganizers50By50()
//  }
//
//  Akka.system.scheduler.schedule(initialDelay = 600.seconds, interval = 12.hours) {
//    Logger.info("Scheduler.updateGeographicPointOfEvents started")
//    scheduler.updateGeographicPointOfEvents50By50()
//  }
}