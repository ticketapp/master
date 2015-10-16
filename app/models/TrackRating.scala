package models

import java.util.UUID
import javax.inject.Inject



import controllers.DAOException
import play.api.Logger

import play.api.db.slick.DatabaseConfigProvider
import services.Utilities
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.model.ForeignKeyAction

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
/*
CREATE TABLE tracksRating (
  tableId                 SERIAL PRIMARY KEY,
  userId                  VARCHAR(255) REFERENCES users_login (userId) NOT NULL,
  trackId                 UUID REFERENCES tracks (trackId) NOT NULL,
  ratingUp                INT,
  ratingDown              INT,
  reason                  CHAR
);
 */

case class TrackRating(userId: UUID,
                      trackId: UUID,
                      ratingUp: Int,
                      ratingDown: Int,
                      reason: Char)


class TrackRatingMethods @Inject()(dbConfigProvider: DatabaseConfigProvider,
                             val utilities: Utilities) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._

  class TrackRatings(tag: Tag) extends Table[TrackRating](tag, "trackratings") {
    def tableId = column[Long]("tableid", O.PrimaryKey, O.AutoInc)
    def userId = column[UUID]("userid")
    def trackId = column[UUID]("trackid")
    def ratingUp = column[Int]("ratingup")
    def ratingDown = column[Int]("ratingdown")
    def reason = column[Char]("reason")

    def * = (userId, trackId, ratingUp, ratingDown, reason) <> ((TrackRating.apply _).tupled, TrackRating.unapply)
  }

  lazy val trackRatings = TableQuery[TrackRatings]
/*
  def upsertRatingUp(userId: UUID, trackId: UUID, rating: Int): Try[Boolean] = Try {
//    trackRatings.insertOrUpdate()

    updateRating(trackId, rating)

    DB.withConnection { implicit connection =>
      SQL("SELECT upsertTrackRatingUp({userId}, {trackId}, {rating})")
        .on(
          'userId -> userId,
          'trackId -> trackId,
          'rating -> rating)
        .execute()
    }
  }

  def upsertRatingDown(userId: UUID, trackId: UUID, rating: Int, reason: Option[Char]): Try[Boolean] = Try {
    updateRating(trackId, rating)

    DB.withConnection { implicit connection =>
      SQL("SELECT upsertTrackRatingDown({userId}, {trackId}, {rating}, {reason})")
        .on(
          'userId -> userId,
          'trackId -> trackId,
          'rating -> math.abs(rating),
          'reason -> reason)
        .execute()
    }
  }

  //  private val ratingParser: RowParser[(Int, Int)] = {
  //    get[Option[Int]]("ratingUp") ~
  //      get[Option[Int]]("ratingDown") map {
  //      case ratingUp ~ ratingDown => (ratingUp.getOrElse(0), ratingDown.getOrElse(0))
  //    }
  //  }

  def updateRating(trackId: UUID, ratingToAdd: Int): Try[Double] = Try {
    getRating(trackId) match {
      case Success(Some(actualRating)) =>
        var actualRatingUp = actualRating._1
        var actualRatingDown = actualRating._2

        ratingToAdd match {
          case ratingUp if ratingUp > 0 => actualRatingUp = actualRatingUp + ratingUp
          case ratingDown if ratingDown <= 0 => actualRatingDown = actualRatingDown + math.abs(ratingDown)
        }

        val confidence = calculateConfidence(actualRatingUp, actualRatingDown)

        persistUpdateRating(trackId, actualRatingUp, actualRatingDown, confidence) match {
          case Success(1) =>
            confidence
          case Failure(exception) =>
            Logger.error(s"Track.updateRating: persistUpdateRating: error while updating with trackId: trackId", exception)
            throw exception
          case _ =>
            throw new DAOException("Track.updateRating: persistUpdateRating")
        }

      case Failure(exception) =>
        Logger.error(s"Track.updateRating: error while updating with trackId: trackId", exception)
        throw exception

      case _ =>
        Logger.error(s"Track.updateRating: error while updating with trackId: $trackId")
        throw new DAOException("Track.updateRating")
    }
  }

  def persistUpdateRating(uuid: UUID, actualRatingUp: Int, actualRatingDown: Int, confidence: Double): Future[Int] = {
    val query = tracks
      .filter(_.uuid === uuid)
      .map(track => (track.ratingUp, track.ratingDown, track.confidence))
      .update((actualRatingUp, actualRatingDown, Option(confidence)))

    db.run(query)
  }

  def getRating(uuid: UUID): Future[Option[(Int, Int)]] = {
    val query = tracks
      .filter(_.uuid === uuid)
      .map(track => (track.ratingUp, track.ratingDown))
    db.run(query.result.headOption)
  }

  def getRatingForUser(userId: UUID, trackId: UUID): Future[Option[(Int, Int)]] = {
    //    val query = tracks
    //      .filter(_.uuid === uuid)
    //      .map(track => (track.ratingUp, track.ratingDown))
    //    DB.withConnection { implicit connection =>
    //      SQL("SELECT ratingUp, ratingDown FROM tracksRating WHERE userId = {userId} AND trackId = {trackId}")
    //        .on(
    //          'userId -> userId,
    //          'trackId -> trackId)
    //        .as(ratingParser.singleOpt)
    //    }
    Future { Option((0, 0))}
  }

  def deleteRatingForUser(userId: UUID, trackId: UUID): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM tracksRating WHERE userId = {userId} AND trackId = {trackId}""".stripMargin)
        .on('userId -> userId,
          'trackId -> trackId)
        .executeUpdate()
    }
  }*/
}