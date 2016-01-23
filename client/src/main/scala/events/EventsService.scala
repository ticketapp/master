package events

import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.{Factory, Service, injectable}
import org.scalajs.dom.console
import upickle.Js

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.{UndefOr, Dictionary, Date, JSON}
import scala.util.{Failure, Success, Try}
import upickle.default._

@injectable("eventhttpService")
class EventsService(http: HttpService) extends Service {


 /* implicit val dateTimeWriter = upickle.default.Writer[Date]{
      case t => Js.Str(t.toString)
    }
  implicit val dateTimeReader = upickle.default.Reader[Date]{
      case Js.Str(str) =>
        new Date(str)
    }*/
  /*implicit val OptionStringReader = upickle.default.Reader[Option[String]]{
      case Js.Str(str) =>
        Some(str)
      case _ =>
        None
    }
  implicit val OptionDateReader = upickle.default.Reader[Option[Date]]{
      case Js.Str(str) =>
        Some(read[Date](str))
      case _ =>
        None
    }
  implicit val OptionLongReader = upickle.default.Reader[Option[Long]]{
      case Js.Str(str) =>
        Some(str.toLong)
      case _ =>
        None
    }*/

 /* implicit val eventReader = upickle.default.Reader[Happening]{
    case Js.Str(str) =>
      console.log(str)
      val eventFromString =  JSON.parse(str).asInstanceOf[js.Dictionary[js.Any]]
      val id = if(eventFromString.keys.filter(_ == "id").toSeq.isEmpty) None else Some(eventFromString("id").toString.toLong)
      val facebookId = if(eventFromString.keys.filter(_ == "facebookId").toSeq.isEmpty) None else Some(eventFromString("facebookId").toString)
      val imagePath = if(eventFromString.keys.filter(_ == "imagePath").toSeq.isEmpty) None else Some(eventFromString("imagePath").toString)
      val ticketSellers = if(eventFromString.keys.filter(_ == "ticketSellers").toSeq.isEmpty) None else Some(eventFromString("ticketSellers").toString)
      val description = if(eventFromString.keys.filter(_ == "description").toSeq.isEmpty) None else Some(eventFromString("description").toString)
      val endTime = if(eventFromString.keys.filter(_ == "endTime").toSeq.isEmpty) None else Some(new Date(eventFromString("endTime").toString))
      val tariffRange = if(eventFromString.keys.filter(_ == "tariffRange").toSeq.isEmpty) None else Some(eventFromString("tariffRange").toString)

      Happening(
        id = id,
        facebookId = facebookId,
        isPublic = eventFromString("isPublic").toString.toBoolean,
        isActive = eventFromString("isActive").toString.toBoolean,
        name = eventFromString("name").toString,
        geographicPoint = Geometry(point = eventFromString("geographicPoint").toString),
        description = description,
        startTime = new Date(eventFromString("startTime").toString),
        endTime = endTime,
        ageRestriction = eventFromString("ageRestriction").toString.toInt,
        tariffRange = tariffRange,
        ticketSellers = ticketSellers,
        imagePath = imagePath
      )
  }
*/
  // fake data
  val stringEvent = "{" +
    "\"id\":1," +
    "\"facebookId\":\"1530466007244553\"," +
    "\"isPublic\":true," +
    "\"isActive\":true," +
    "\"name\":\"GENERAL ELEKTRIKS en concert | ESPACE JULIEN\"," +
    "\"geographicPoint\":\"POINT (-84 30)\"," +
    "\"description\":\"<div class='column large-12'>La SAS et l’Espace Julien, en accord avec W Spectacles, " +
    "présentent :<br/><br/></div><div class='column large-12'>GENERAL ELEKTRIKS en concert à MARSEILLE.<br/>➔ jeudi 17 Mars 2016 à l'Espace Julien.<br/>+ " +
    "'>general-elektriks.com</a><br/><br/></div><div class='column large-12'>➔ PARTENAIRES :<br/>Merci La prévente étudiante<br/><br/></d" +
    "iv><div class='column large-12'>➔ LE LIEU :<br/>Espace Julien<br/>39 Cours Julien, 13006 Marseille <br/><br/></div><div class='column large-12" +
    "'>➔ CONTACTS : <br/>La SAS<br/>22 rue Robert, 13007 Marseille <br/>Infos : lasasmarseille[at]<a href='http://gmail.com'>gmail.com</a><br/>Partenariat" +
    "s : vivian.lasas[at]<a href='http://gmail.com'>gmail.com</a><br/>+33 4 91 33 06 83<br/><a href='http://lasasconcerts.com'>lasasconcerts.com</a></div>\"," +
    "\"startTime\":1458241200000,\"" +
    "ageRestriction\":16," +
    "\"imagePath\":\"https://scontent.xx.fbcdn.net/hphotos-xta1/v/t1.0-9/s720x720/12122508_11566…_8741765599189185794_n.jpg?o" +
    "h=434fab08559418f298c67152dd10dced&oe=573BECB2\"}"

  val eventsPath = "http://localhost:9000/events?offset=0&numberToReturn=1&geographicPoint=0,0"

  def all(): Array[Happening] = {
    println("calling all from EventService")
    Array.empty
  }

  /*def get(id: Int): Happening = {
    println(s"calling get in EventService for id = $id")
    events(id)
  }*/

  def getAll(): Future[Seq[Happening]] = {
   /* val getFuture = http.get[js.Any](eventsPath) // implicit conversion occurs here.
    getFuture.onFailure {
      case err =>
        loading.hide()
        loading.show(LoadingOpt("Please check your network connection", duration = 3000))
    }*/

    /*val d = read[Happening](stringEvent)
    Future(Seq(d))*/
    Future(Seq.empty[Happening])
  }

}

@injectable("eventhttpService")
class EventsServiceFactory(http: HttpService) extends Factory[EventsService] {
  def apply(): EventsService = new EventsService(http)
}

