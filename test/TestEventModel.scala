import java.text.{DateFormat, SimpleDateFormat}
import controllers.DAOException
import models.Event._
import models.{Organizer, Address, Event}
import org.postgresql.util.PSQLException
import org.scalatest.time.{Span, Seconds}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.concurrent.Future
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits._
import org.scalatest.concurrent.ScalaFutures._
import java.util.Date

class TestEventModel extends PlaySpec with OneAppPerSuite {

  "An event" must {
    val event = Event(None, None, isPublic = true, isActive = true, "event name", Option("(5.4,5.6)"),
      Option("description"), new Date(), Option(new Date()), 16, None, None, None, List.empty, List.empty,
      List.empty, List.empty, List.empty, List.empty)

    "be able to be saved and deleted in database" in {
      val eventId = save(event).get

      find(eventId).get.name mustBe "event name"

      delete(eventId) mustBe 1
      //find(eventId) mustEqual Option(event.copy(eventId = Some(eventId)))
      //pb with dates
    }

    "be able to be followed and unfollowed by a user" in {
      follow("userTestId", 1) shouldBe a [Success[Option[Long]]]
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollow("userTestId", 1) mustBe 1
    }

    "not be able to be followed twice" in {
      follow("userTestId", 1) shouldBe a [Success[Option[Long]]]
      follow("userTestId", 1) shouldBe a [Failure[PSQLException]]
      unfollow("userTestId", 1) mustBe 1
    }

    "be able to be found on facebook by a facebookId" in {
      //2015-05-03 07:07:46.2433
      val dateFormat: DateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS")
      dateFormat.format(new Date())
      val expectedEvent = Event(None, Some("809097205831013"), isPublic = true, isActive = true,
        "Mad Professor vs Prince Fatty - Dub Attack Tour @ Club Transbo", None,
        Some("""<div class='column large-12'>MAD PROFESSOR DUBBING LIVE ARIWA CLASSICS AND MASSIVE ATTACKS<br/><br/></div><div class='column large-12'>Découvrez une rencontre au sommet de la scène dub-reggae anglaise entre le légendaire Mad Professor et le surpuissant Prince Fatty. Un mélange entre classiques du label Ariwa, reprises de Massive Attack et raretés, dubbés par des poids lourds du genre !<br/><br/></div><div class='column large-12'>Mad Professor & Prince Fatty vous invitent à découvrir sur scène l'ambiance d'un studio d'enregistrement des années 70 où règnent table multi-pistes & effets vintage en tout genre !<br/><br/></div><div class='column large-12'>__________________________________________________________<br/><br/></div><div class='column large-12'>VENDREDI 05 JUIN 2015 / 19h30<br/><br/></div><div class='column large-12'>Club Transbo, 3 bd Stalingrad 69100 Villeurbanne<br/><br/></div><div class='column large-12'>15€ en prévente* / 18€ sur place<br/><br/></div><div class='column large-12'>* Tarif des préventes hors frais de loc. et dans la limite des places disponibles.<br/><br/></div><div class='column large-12'>Réservation : Digitick et Fnac<br/><br/></div><div class='column large-12'>PRÉVENTES : <a href='http://bit.ly/dubattack'>bit.ly/dubattack</a><br/><br/></div><div class='column large-12'>__________________________________________________________<br/><br/></div><div class='column large-12'>Proposé par Collectif Démon d'Or & Totaal Rez<br/>__________________________________________________________<br/><br/></div><div class='column large-12'>PARTENAIRES :<br/><br/></div><div class='column large-12'>▲ Beyeah : <a href='http://beyeah.net'>beyeah.net</a><br/>▲ BF2D : <a href='http://bf2d.net'>bf2d.net</a><br/>▲ Crédit Mutuel : <a href='http://riffx.fr'>riffx.fr</a><br/>▲ Culture Dub : <a href='http://culturedub.com'>culturedub.com</a><br/>▲ Digitick : <a href='http://digitick.com'>digitick.com</a><br/>▲ Electro News : <a href='http://electro-news.fr'>electro-news.fr</a><br/>▲ Kiblind : <a href='http://kiblind.com'>kiblind.com</a><br/>▲ Le Mauvais Coton : <a href='http://lemauvaiscoton.fr'>lemauvaiscoton.fr</a><br/>▲ Musical Echoes - <a href='http://musicalechoes.fr'>musicalechoes.fr</a><br/>▲ Paperboys : <a href='http://paperboys.fr'>paperboys.fr</a><br/>▲ Radio Electro Lyon : <a href='http://radioelectrolyon.fr'>radioelectrolyon.fr</a><br/>▲ Talawa - <a href='http://talawa.fr'>talawa.fr</a><br/>▲ Teck'Yo : <a href='http://teckyo.com'>teckyo.com</a><br/>▲ Trax : <a href='http://magazinetrax.com'>magazinetrax.com</a><br/>▲ Uber : <a href='http://facebook.com/uberfrance'>facebook.com/uberfrance</a><br/>▲ Zyva : <a href='http://zyvamusic.com'>zyvamusic.com</a></div>"""),
        new Date(), None, 16, Some("15.0-18.0"), None,
        Some("""https://scontent.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/s720x720/1484676_918815728149949_7634659918178640444_n.jpg?oh=4d769b8acff3ad1c5b7723fb26ca1c9f&oe=55DA197C"""),
        List(Organizer(None, Some("463335063676731"), "Totaal Rez", None, None, Some("09 54 84 74 86"), None,
        Some("http://www.totaalrez.com"), verified = false,
        Some("""https://scontent.xx.fbcdn.net/hphotos-xpa1/v/t1.0-9/s720x720/1902843_753269114683323_58996057_n.jpg?oh=ad289a02ea56b8c5baa3151ba87d1076&oe=55C73A3F"""),
        None, Some(Address(None, None, Some("Lyon"), None, None)))), List(), List(), List(Address(None,None,Some("Villeurbanne"),
        Some("69100"),Some("3 boulevard de la bataille de Stalingrad"))), List(), List())

      // findEventOnFacebookByFacebookId("809097205831013") shouldBe a [Future[Event]]
      whenReady (findEventOnFacebookByFacebookId("809097205831013"), timeout(Span(5, Seconds))) { event =>
        val dateWhileThisProblemWithDatesIsNotSolved = new Date()
        event.copy(startTime = dateWhileThisProblemWithDatesIsNotSolved) mustBe
          expectedEvent.copy(startTime = dateWhileThisProblemWithDatesIsNotSolved)
      }
    }
  }
}
