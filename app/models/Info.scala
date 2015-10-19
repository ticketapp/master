package models

import javax.inject.Inject

import controllers.DAOException
import play.api.db._
import play.api.Play.current
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}


import play.api.libs.json.Json
import services.{MyPostgresDriver, Utilities}

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

class InfoMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val placeMethods: PlaceMethods,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] {

//
//  def findAll(): Seq[Info] = try {
//    DB.withConnection { implicit connection =>
//      SQL("""SELECT * FROM infos""")
//        .as(InfoParser.*)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Info.findAll: " + e.getMessage)
//  }
//
//  def find(id: Long): Option[Info] = try {
//    DB.withConnection { implicit connection =>
//      SQL("SELECT * FROM infos WHERE id = {id}")
//        .on('id -> id)
//        .as(InfoParser.singleOpt)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Info.find: " + e.getMessage)
//  }
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


