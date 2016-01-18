package others

import java.util.UUID
import javax.inject.Inject

import artistsDomain.ArtistMethods
import genresDomain.GenreMethods
import play.api.db.slick.DatabaseConfigProvider
import services.Utilities

case class Mail(id: Option[Long], subject: String, message: String, read: Boolean, userId: Option[UUID])

class MailMethods @Inject()(dbConfigProvider: DatabaseConfigProvider,
                            val artistMethods: ArtistMethods,
                            val genreMethods: GenreMethods) {
//  private val mailParser = {
//    get[Long]("id") ~
//      get[String]("subject") ~
//      get[String]("message") ~
//      get[Boolean]("read") ~
//      get[UUID]("userId") map {
//      case id ~ subject ~ message ~ read ~ userId=>
//        Mail(Option(id), subject, message, read, Option(userId))
//    }
//  }
//
//  def mailFormApply(subject: String, message: String) = new Mail(None, subject, message, false, None)
//  def mailFormUnapply(mail: Mail) = Some((mail.subject, mail.message))
//
//  def findAll: List[Mail] = try {
//    DB.withConnection { implicit connection =>
//      SQL("SELECT * FROM receivedMails").as(mailParser.*)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Mail.findAll: " + e.getMessage)
//  }
//
//  def save(mail: Mail): Option[Long] = try {
//    DB.withConnection { implicit connection =>
//      SQL(
//        """INSERT INTO receivedMails(subject, message)
//          | VALUES ({subject}, {message})""".stripMargin)
//        .on(
//          'subject -> mail.subject,
//          'message -> mail.message)
//        .executeInsert()
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Mail.save: " + e.getMessage)
//  }
}

