package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.Play.current
import services.Utilities._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

case class Genre (genreId: Long, name: String)

object Genre {
  def formApply(name: String) = new Genre(-1L, name)
  def formUnapply(genre: Genre) = Some(genre.name)

  private val GenreParser: RowParser[Genre] = {
    get[Long]("genreId") ~
      get[String]("name") map {
      case genreId ~ name => Genre(genreId, name)
    }
  }

  def findAll: Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM genres").as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Genre.findAll: " + e.getMessage)
  }

  def findAllByEvent(eventId: Long): Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsGenres eA
             INNER JOIN genres a ON a.genreId = eA.genreId where eA.eventId = {eventId}""")
        .on('eventId -> eventId)
        .as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Genre.findAllByEvent: " + e.getMessage)
  }

  def findAllByArtist(artistFacebookId: Int): Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM artistsGenres gA
             INNER JOIN genres g ON g.genreId = gA.genreId where gA.artistFacebookId = {artistFacebookId}""")
        .on('artistFacebookId -> artistFacebookId)
        .as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot get all genres by artist: " + e.getMessage)
  }

  def find(genreId: Long): Option[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM genres WHERE genreId = {genreId}")
        .on('genreId -> genreId)
        .as(GenreParser.singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Genre.find: " + e.getMessage)
  }

  def findAllContaining(pattern: String): Seq[Genre] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM genres WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
        .on('patternLowCase -> pattern.toLowerCase)
        .as(GenreParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Genre.findAllContaining: " + e.getMessage)
  }
  
  def save(genre: Genre): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertGenre({name})""")
        .on('name -> genre.name)
        .as(scalar[Option[Long]].single)
        //.execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot create genre: " + e.getMessage)
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

  def saveWithEventRelation(genre: Genre, eventId: Long): Option[Long] = {
    save(genre) match {
      case Some(genreId: Long) => saveEventGenreRelation(eventId, genreId)
      case _ => None
    }
  }

  def saveEventGenreRelation(eventId: Long, genreId: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO eventsGenres (eventId, genreId)
          VALUES ({eventId}, {genreId})""")
        .on(
          'eventId -> eventId,
          'genreId -> genreId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("saveEventGenreRelation: " + e.getMessage)
  }

  def saveWithArtistRelation(genre: Genre, artistId: Int): Boolean = {
    save(genre) match {
      case Some(genreId: Long) => saveArtistGenreRelation(artistId, genreId.toInt)
      case _ => false
    }
  }

  def saveArtistGenreRelation(artistId: Int, genreId: Int): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertOrUpdateArtistGenreRelation({artistId}, {genreId})""")
        .on(
          'artistId -> artistId,
          'genreId -> genreId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Genre.saveArtistGenreRelation: " + e.getMessage)
  }

  def saveGenreForArtistInFuture(genreName: Option[String], artistId: Int): Unit = {
    Future {
      genreName match {
        case Some(genreFound) if genreFound.nonEmpty =>
          saveWithArtistRelation(new Genre(-1, genreFound), artistId)
      }
    }
  }

  def deleteGenre(genreId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM genres WHERE genreId={genreId}""")
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
    case e: Exception => throw new DAOException("Cannot follow genre: " + e.getMessage)
  }

}