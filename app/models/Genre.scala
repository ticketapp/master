package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Genre (genreId: Long, name: String)

object Genre {
  def formApply(name: String) = new Genre(-1L, name)
  def formUnapply(genre: Genre) = Some(genre.name)

  private val GenreParser: RowParser[Genre] = {
    get[Long]("genreId") ~
      get[String]("name") map {
      case genreId ~ name =>
        Genre(genreId, name)
    }
  }

  def findAll(): List[Genre] = {
    DB.withConnection { implicit connection =>
      SQL("select * from genres").as(GenreParser.*)
    }
  }

  def findAllByEvent(event: Event): List[Genre] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsGenres eA
             INNER JOIN genres a ON a.genreId = eA.genreId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(GenreParser.*)
    }
  }

  def findAllByArtist(artistId: Long): List[Genre] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM artistsGenres aG
             INNER JOIN genres g ON g.genreId = aG.genreId where aG.artistId = {artistId}""")
        .on('artistId -> artistId)
        .as(GenreParser.*)
    }
  }

  def find(genreId: Long): Option[Genre] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from genres WHERE genreId = {genreId}")
        .on('genreId -> genreId)
        .as(GenreParser.singleOpt)
    }
  }

  def findAllContaining(pattern: String): Seq[Genre] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM genres WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(GenreParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Genre.findAllContaining: " + e.getMessage)
    }
  }

  def saveGenreAndArtistRelation(genre: Genre, id: Long): Option[Long] = {
   try {
      DB.withConnection { implicit connection =>
        SQL( s"""INSERT into genres(name)
        values ({name})"""
        ).on(
            'name -> genre.name
          ).executeInsert() match {
          case Some(x: Long) => saveArtistGenreRelation(id, x)
          case _ => None
        }
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create genre: " + e.getMessage)
    }
  }

  def saveArtistGenreRelation(artistId: Long, genreId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO artistsGenres (artistId, genreId)
            VALUES ({artistId}, {genreId})""").on(
            'artistId -> artistId,
            'genreId -> genreId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("saveArtistGenreRelation: " + e.getMessage)
    }
  }

  def deleteGenre(genreId: Long): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("""DELETE FROM genres WHERE genreId={genreId}""").on('genreId -> genreId).executeUpdate()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot delete genre: " + e.getMessage)
    }
  }

  def followGenre(userId : Long, genreId : Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into genreFollowed(userId, genreId) values ({userId}, {genreId})").on(
          'userId -> userId,
          'genreId -> genreId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow genre: " + e.getMessage)
    }
  }
}