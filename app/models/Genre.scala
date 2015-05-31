package models

import java.util.UUID

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.Play.current
import services.Utilities._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.{Failure, Try}

case class Genre (genreId: Option[Long], name: String, icon: Option[String] = None)

object Genre {
  def formApply(name: String) = new Genre(None, name)
  def formUnapply(genre: Genre) = Some(genre.name)

  def genresStringToGenresSets(l: Option[String]) = l

  private val GenreParser: RowParser[Genre] = {
    get[Long]("genreId") ~
      get[String]("name") ~
      get[Option[String]]("icon") map {
      case genreId ~ name ~ icon => Genre(Option(genreId), name, icon)
    }
  }

  def findAll: Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM genres")
        .as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.findAll: " + e.getMessage)
  }

  def findAllByEvent(eventId: Long): Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM eventsGenres eA
          | INNER JOIN genres a ON a.genreId = eA.genreId
          |   WHERE eA.eventId = {eventId}""".stripMargin)
        .on('eventId -> eventId)
        .as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.findAllByEvent: " + e.getMessage)
  }

  def findAllByArtist(artistId: Int): Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM artistsGenres aG
          | INNER JOIN genres g ON g.genreId = aG.genreId
          |   WHERE aG.artistId = {artistId}""".stripMargin)
        .on('artistId -> artistId)
        .as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.findAllByArtist: " + e.getMessage)
  }

  def find(genreId: Long): Option[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM genres WHERE genreId = {genreId}")
        .on('genreId -> genreId)
        .as(GenreParser.singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.find: " + e.getMessage)
  }

  def findAllContaining(pattern: String): Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM genres
          | WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%'
          | LIMIT 12""".stripMargin)
        .on('patternLowCase -> pattern.toLowerCase)
        .as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.findAllContaining: " + e.getMessage)
  }
  
  def save(genre: Genre): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertGenre({name}, {icon})""")
        .on('name -> genre.name,
            'icon -> genre.icon)
        .as(scalar[Option[Long]].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.save: " + e.getMessage)
  }

  def findGenreId(genreName: String): Option[Long] = try {
    DB.withConnection { implicit connection =>
        SQL( """SELECT genreId FROM genres WHERE name = {name}""")
          .on('name -> genreName)
          .as(scalar[Option[Long]].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findGenreId: " + e.getMessage)
  }

  def saveWithEventRelation(genre: Genre, eventId: Long): Int = {
    save(genre) match {
      case Some(genreId: Long) => saveEventRelation(eventId, genreId)
      case _ => 0
    }
  }

  def saveEventRelation(eventId: Long, genreId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO eventsGenres (eventId, genreId)
            |  VALUES({eventId}, {genreId})""".stripMargin)
        .on(
          'eventId -> eventId,
          'genreId -> genreId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.saveEventRelation: " + e.getMessage)
  }

  def deleteEventRelation(eventId: Long, genreId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM eventsGenres WHERE eventId = {eventId} AND genreId = {genreId}""")
        .on(
          'eventId -> eventId,
          'genreId -> genreId)
        .executeUpdate()
    }
  }

  def saveWithArtistRelation(genre: Genre, artistId: Long): Boolean = {
    save(genre) match {
      case Some(genreId: Long) => saveArtistRelation(artistId, genreId.toInt)
      case _ => false
    }
  }

  def saveArtistRelation(artistId: Long, genreId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertOrUpdateArtistGenreRelation({artistId}, {genreId})""")
        .on(
          'artistId -> artistId,
          'genreId -> genreId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.saveArtistRelation: " + e.getMessage)
  }

  def deleteArtistRelation(artistId: Long, genreId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM artistsGenres WHERE artistId = {artistId} AND genreId = {genreId}""")
        .on(
          'artistId -> artistId,
          'genreId -> genreId)
        .executeUpdate()
    }
  }

  def saveGenreForArtistInFuture(genreName: Option[String], artistId: Long): Unit = {
    Future {
      genreName match {
        case Some(genreFound) if genreFound.nonEmpty =>
          saveWithArtistRelation(new Genre(None, genreFound), artistId)
      }
    }
  }

  def saveWithTrackRelation(genre: Genre, trackId: UUID, weight: Long): Try[Boolean] = save(genre) match {
    case Some(genreId: Long) => saveTrackRelation(trackId, genreId, weight)
    case _ => Failure(new DAOException("Genre.saveWithTrackRelation"))
  }

  def saveTrackRelation(trackId: UUID, genreId: Long, weight: Long): Try[Boolean] = Try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT upsertTrackGenreRelation({trackId}, {genreId}, {weight})""")
        .on(
          'trackId -> trackId,
          'genreId -> genreId,
          'weight -> weight)
        .execute()
    }
  }

  def deleteTrackRelation(trackId: UUID, genreId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM tracksGenres WHERE trackId = {trackId} AND genreId = {genreId}""")
        .on(
          'trackId -> trackId,
          'genreId -> genreId)
        .executeUpdate()
    }
  }

  def delete(genreId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM genres WHERE genreId = {genreId}""")
        .on('genreId -> genreId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot delete genre: " + e.getMessage)
  }

  def followGenre(userId : Long, genreId : Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO genreFollowed(userId, genreId) VALUES ({userId}, {genreId})")
        .on(
          'userId -> userId,
          'genreId -> genreId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.followGenre: " + e.getMessage)
  }

  def genresStringToGenresSet(genres: Option[String]): Set[Genre] = genres match {
    case None => Set.empty
    case Some(genres: String) =>
      val lowercaseGenres = genres.toLowerCase
      val genresSplitByCommas = lowercaseGenres.split(",")
      if (genresSplitByCommas.length > 1) {
        genresSplitByCommas.map { genreName => Genre(None, genreName.stripSuffix(",").trim, None) }.toSet
      } else {
        """([%/+\.;]|& )""".r
          .split(lowercaseGenres)
          .map { _.trim }
          .filterNot(_ == "")
        match {
          case list if list.length != 1 =>
            list.map { genreName => new Genre(None, genreName) }.toSet
          case listOfOneItem =>
            listOfOneItem(0) match {
              case genre if genre.contains("'") || genre.contains("&") || genre.contains("musique") ||
                genre.contains("musik") || genre.contains("music") =>
                Set(new Genre(None, genre))
              case genreWithoutForbiddenChars =>
                genreWithoutForbiddenChars
                  .split("\\s+")
                  .map { genreName => new Genre(None, genreName.stripSuffix(".")) }
                  .filterNot(_.name == "")
                  .toSet
            }
        }
      }
  }
}