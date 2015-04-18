package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.Play.current

case class Mail(id: Option[Long], subject: String, message: String, read: Boolean, userId: Option[String])

object Mail {
  private val mailParser = {
    get[Long]("id") ~
      get[String]("subject") ~
      get[String]("message") ~
      get[Boolean]("read") ~
      get[String]("userId") map {
      case id ~ subject ~ message ~ read ~ userId=>
        Mail(Option(id), subject, message, read, Option(userId))
    }
  }

  def mailFormApply(subject: String, message: String) = new Mail(None, subject, message, false, None)
  def mailFormUnapply(mail: Mail) = Some((mail.subject, mail.message))

  def findAll: List[Mail] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM receivedMails").as(mailParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Mail.findAll: " + e.getMessage)
  }

  def save(mail: Mail): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO receivedMails(subject, message)
          | VALUES ({subject}, {message})""".stripMargin)
        .on(
          'title -> mail.subject,
          'content -> mail.message)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Mail.save: " + e.getMessage)
  }
}
