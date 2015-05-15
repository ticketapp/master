package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import controllers.DAOException
import java.util.Date

import scala.util.Random

case class Tool (tools: String, userId: Long)


object Tool {
  implicit val toolWrites = Json.writes[Tool]

  /*private val ToolParser: RowParser[Tool] = {
    get[String]("name") ~
    get[Long]("userId") map {
      case name ~ userId =>
        Tool(name, userId)
    }
  }*/

  def formApply(tools: String, userId: Long): Tool = new Tool(tools, userId)

  def formUnapply(tool: Tool): Option[(String, Long)] = Some((tool.tools, tool.userId))

  def findByUserId(userId: Long): String = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT tools
             FROM usersTools
             WHERE userId = {userId}"""
      ).on('userId -> userId)
        .as(scalar[String].single)
    }
  }

  def save(tool: Tool): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """INSERT INTO tools(name userId)
            VALUES({name}, {userId})
          """).on(
            'name -> tool.tools,
            'userId -> tool.userId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save tool: " + e.getMessage)
    }
  }
}
