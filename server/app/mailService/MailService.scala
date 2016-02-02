package mailService

import javax.inject.Inject

import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.Controller


class MailService @Inject() (mailerClient: MailerClient, recipient: String) extends Controller {

  val destinationMail = Seq("<simongarnier07@hotmail.fr>")

  def sendNotificationMail(content: String): Unit = {

    val email = Email(
      subject = "Des Toits En Ville : vous avez une notification",
      from = "notifications.destoitsenville@gmail.com",
      to = destinationMail,
      bodyHtml = Some(
        s"""<html><body><p>$content</body></html>""".stripMargin)
    )

    mailerClient.send(email)
  }
}