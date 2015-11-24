package tracksDomain

import java.util.UUID
import javax.inject.Inject
import application.DAOException
import database.{MyPostgresDriver, MyDBTableDefinitions}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import MyPostgresDriver.api._
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


case class TrackRating(userId: UUID,
                      trackId: UUID,
                      ratingUp: Int,
                      ratingDown: Int,
                      reason: Option[Char] = None)

case class Rating(ratingUp: Int, ratingDown: Int)


class TrackRatingMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                   val utilities: Utilities,
                                   val trackMethods: TrackMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions {

  def upsertRatingForAUser(userId: UUID, trackId: UUID, rating: Int): Future[Int] = {
    val newRating = rating match {
      case ratingUp if ratingUp > 0 =>
        TrackRating(userId = userId, trackId = trackId, ratingUp = rating, ratingDown = 0)
      case ratingDown if ratingDown <= 0 =>
        TrackRating(userId = userId, trackId = trackId, ratingUp = 0, ratingDown = rating)
    }

    updateGeneralRating(trackId, rating)

    db.run((for {
      userTrackRatingFound <- trackRatings.filter(trackRating => trackRating.userId === userId &&
        trackRating.trackId === trackId).result.headOption
      result <- userTrackRatingFound
        .map(DBIO.successful)
        .getOrElse(trackRatings returning trackRatings.map(_.tableId) += newRating)
    } yield result match {
      case ratingToUpdate: TrackRating =>
        db.run(trackRatings
          .filter(trackRating => trackRating.userId === userId && trackRating.trackId === trackId)
          .update(ratingToUpdate.copy(
            ratingUp = ratingToUpdate.ratingUp + newRating.ratingUp,
            ratingDown = ratingToUpdate.ratingDown + newRating.ratingDown)))
      case long: Long =>
          Future(1)
    }).transactionally)
      .flatMap(numberOfUpdatedRows => numberOfUpdatedRows)
  }

  def updateGeneralRating(trackId: UUID, ratingToAdd: Int): Future[Try[Double]] = getGeneralRating(trackId) flatMap {
    case Some(actualRating) =>
      var actualRatingUp = actualRating.ratingUp
      var actualRatingDown = actualRating.ratingDown

      ratingToAdd match {
        case ratingUp if ratingUp > 0 => actualRatingUp = actualRatingUp + ratingUp
        case ratingDown if ratingDown <= 0 => actualRatingDown = actualRatingDown + math.abs(ratingDown)
      }

      val confidence = trackMethods.calculateConfidence(actualRatingUp, actualRatingDown)

      persistUpdateRating(trackId, actualRatingUp, actualRatingDown, confidence) map {
        case 1 =>
          Success(confidence)
        case _ =>
          Failure(DAOException(s"TrackRating.updateRating: error while updating with trackId: $trackId"))
      }
    case None =>
      Future(Failure(DAOException(s"TrackRating.updateRating: no track with id: $trackId")))
  }

  def persistUpdateRating(uuid: UUID, actualRatingUp: Int, actualRatingDown: Int, confidence: Double): Future[Int] = {
    val query = tracks
      .filter(_.uuid === uuid)
      .map(track => (track.ratingUp, track.ratingDown, track.confidence))
      .update((actualRatingUp, actualRatingDown, confidence))

    db.run(query)
  }

  def getGeneralRating(uuid: UUID): Future[Option[Rating]] = db.run(
    tracks
      .filter(_.uuid === uuid)
      .map(track => (track.ratingUp, track.ratingDown))
      .result
      .headOption) map(_ map Rating.tupled)

  def getRatingForUser(userId: UUID, trackId: UUID): Future[Option[Rating]] = db.run(
    trackRatings
      .filter(_.userId === userId)
      .map(trackRating => (trackRating.ratingUp, trackRating.ratingDown))
      .result
      .headOption) map(_ map Rating.tupled)

  def getReasonForUser(userId: UUID, trackId: UUID): Future[Option[Char]] = db.run(
    trackRatings
      .filter(_.userId === userId)
      .map(trackRating => trackRating.reason)
      .result
      .headOption) map (_.flatten)

  def deleteRatingForUser(userId: UUID, trackId: UUID): Future[Int] = db.run(
    trackRatings
      .filter(trackRating => trackRating.userId === userId && trackRating.trackId === trackId)
      .delete)
}