package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json


case class Info(id: Long,
                title: String,
                content: String)

object Info {
  private val InfoParser: RowParser[Info] = {
    get[Long]("infoId") ~
    get[String]("title") ~
    get[String]("content") map {
      case infoId ~ title ~ content =>
        Info(infoId, title, content)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(""" SELECT * from infos""").as(InfoParser.*)
    }
  }


  def find(id: Long): Option[Info] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from infos WHERE id = {id}")
        .on('id -> id)
        .as(InfoParser.singleOpt)
    }
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


