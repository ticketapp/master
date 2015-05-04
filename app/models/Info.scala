package models

import controllers.DAOException
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json

/*
  infoId                    SERIAL PRIMARY KEY,
  displayIfConnected        BOOLEAN NOT NULL DEFAULT TRUE,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR,
  animationContent          VARCHAR,
  style                     VARCHAR
 */
case class Info(id: Long,
                displayIfConnected: Boolean,
                title: String,
                content: String,
                animationContent: Option[String],
                animationStyle: Option[String])

object Info {
  private val InfoParser: RowParser[Info] = {
    get[Long]("infoId") ~
    get[Boolean]("displayIfConnected") ~
    get[String]("title") ~
    get[String]("content") ~
    get[Option[String]]("animationContent") ~
    get[Option[String]]("animationStyle") map {
      case infoId ~ displayIfConnected ~ title ~ content ~ animationContent ~ animationStyle =>
        Info(infoId, displayIfConnected, title, content, animationContent, animationStyle)
    }
  }

  def findAll(): Seq[Info] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT * FROM infos""")
        .as(InfoParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Info.findAll: " + e.getMessage)
  }

  def find(id: Long): Option[Info] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM infos WHERE id = {id}")
        .on('id -> id)
        .as(InfoParser.singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("Info.find: " + e.getMessage)
  }
  /*
    def save(name: String) {
      DB.withConnection { implicit connection =>
        SQL("""
              INSERT INTO infos(name, userId)
              VALUES({name}, 0)
            """).on(
          'name -> name)
        .executeUpdate
      }
    }*/
}


