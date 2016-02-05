package attendees

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.libs.ws.WS
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

case class FacebookAttendee(attendeeFacebookId: String, name: String)

case class AttendeeRead(name: String, id: String, rsvp_status: String)

case class FacebookAttendeeEventRelation(attendeeFacebookId: String, eventFacebookId: String, attendeeStatus: Char)

class AttendeeMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[MyPostgresDriver]
  with MyDBTableDefinitions
  with Utilities {

  def save(attendee: FacebookAttendee): Future[Long] =
    db.run(facebookAttendees returning facebookAttendees.map(_.id) += attendee)

  def findByFacebookId(facebookId: String): Future[Option[FacebookAttendee]] =
    db.run(facebookAttendees.filter(_.attendeeFacebookId === facebookId).result.headOption)

  def findAllByEventFacebookId(eventFacebookId: String): Future[Seq[FacebookAttendee]] = {
    val query = for {
      event <- events if event.facebookId === eventFacebookId
      facebookAttendeeEventRelation <- facebookAttendeeEventRelations
      facebookAttendee <- facebookAttendees
      if facebookAttendee.attendeeFacebookId === facebookAttendeeEventRelation.attendeeFacebookId
    } yield facebookAttendee

    db.run(query.result)
  }

  def getAllByEventFacebookId(eventFacebookId: String): Future[Seq[FacebookAttendee]] = WS
    .url("https://graph.facebook.com/" + facebookApiVersion + "/" + eventFacebookId + "/attending")
    .withQueryString(
      "access_token" -> facebookToken,
      "limit" -> "400")
    .get()
    .map(facebookResponse => readJsonAttendees(facebookResponse.json).map(attendeeReadToFacebookAttendee))

  def readJsonAttendees(facebookAttendees: JsValue): Seq[AttendeeRead] = {
    implicit val attendeeRead: Reads[AttendeeRead] = Json.reads[AttendeeRead]
    val readAttendeeRead: Reads[Seq[AttendeeRead]] = Reads.seq(__.read[AttendeeRead])

    val jsonData: JsValue = extractJsonAttendeesFromFacebookJsonResponse(facebookAttendees)

    val validatedJsonAttendees: JsResult[Seq[AttendeeRead]] = jsonData.validate[Seq[AttendeeRead]](readAttendeeRead)

    validatedJsonAttendees match {
      case attendees: JsSuccess[Seq[AttendeeRead]] =>
        attendees.get
      case error: JsError =>
        Logger.error("AttendeesMethods.readJsonAttendees: " + error)
        Seq.empty
    }
  }

  def extractJsonAttendeesFromFacebookJsonResponse(facebookAttendees: JsValue): JsValue = {
    facebookAttendees \ "attending" \ "data" match {
      case JsDefined(attending) => attending
      case _ => facebookAttendees \ "data" match {
        case JsDefined(data) => data
        case _ => JsNull
      }
    }
  }

  def attendeeReadToFacebookAttendee(attendeeRead: AttendeeRead): FacebookAttendee =
    FacebookAttendee(attendeeFacebookId = attendeeRead.id, name = attendeeRead.name)
}
