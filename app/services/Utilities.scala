package services

import java.text.Normalizer

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}


trait Utilities {
  val facebookToken = "1434769156813731%7Cf2378aa93c7174712b63a24eff4cb22c"
  val googleKey = "AIzaSyDx-k7jA4V-71I90xHOXiILW3HHL0tkBYc"
  val echonestApiKey = "3ZYZKU3H3MKR2M59Z"
  val soundCloudClientId = "2a37f8098705604b8679fd079d5ad90f"

  val linkPattern = """((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-z@A-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)""".r
  val geographicPointPattern = """(-?\(\d+\.?\d*,-?\d+\.?\d*\))"""

  val UNIQUE_VIOLATION = "23505"
  val FOREIGN_KEY_VIOLATION = "23503"

  val antarcticPoint = new GeometryFactory().createPoint(new Coordinate(-84, 30))

  val facebookApiVersion = "v2.4"

  def normalizeString(string: String): String = string //Should be replace accentued letters for example?

  def replaceAccentuatedLetters(string: String): String =
    Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")

  def stripChars(s:String, ch:String)= s filterNot (ch contains _)

  def normalizeUrl(website: String): String =
    """(https?:\/\/(www\.)?)|(www\.)""".r.replaceAllIn(website.toLowerCase, p => "").stripSuffix("/")

  def normalizeUrlWithoutLowerCase(website: String): String =
    """(https?:\/\/(www\.)?)|(www\.)""".r.replaceAllIn(website, p => "").stripSuffix("/")

  def normalizeMaybeWebsite(website: Option[String]): Option[String] = website match {
    case Some(websiteMatched) =>
      Option(normalizeUrl(websiteMatched))
    case _ =>
      None
  }

  def optionStringToLowerCaseOptionString(maybeString: Option[String]): Option[String] = maybeString match {
    case Some(string: String) => Option(string.toLowerCase)
    case None => None
  }

  def removeMailFromListOfWebsites(websites: Set[String]): Set[String] = websites.filter(website =>
    website.indexOf("@") == -1)

  def removeSpecialCharacters(string: String): String = string.replaceAll("""[*ù$-+/*_\.\\,#'~´&]""", "")
  
  def unshortLink(url: String): Future[String] = {
    if (url.indexOf("bit.ly") > -1 || url.indexOf("bit.do") > -1 /*|| url.indexOf("t.co") > -1*/ ||
      url.indexOf("lnkd.in") > -1 || url.indexOf("db.tt") > -1 ||  url.indexOf("qr.ae") > -1 ||
      url.indexOf("adf.ly") > -1 || url.indexOf("goo.gl") > -1 || url.indexOf("bitly.com") > -1 ||
      url.indexOf("cur.lv") > -1 || url.indexOf("tinyurl.com") > - 1 || url.indexOf("ow.ly") > -1 ||
      url.indexOf("adcrun.ch") > -1 || url.indexOf("ity.im") > -1 || url.indexOf("q.gs") > -1 ||
      url.indexOf("viralurl.com") > -1 || url.indexOf("is.gd") > -1 || url.indexOf("vur.me") > -1 ||
      url.indexOf("bc.vc") > -1 || url.indexOf("twitthis.com") > -1 || url.indexOf("u.to") > -1 ||
      url.indexOf("j.mp") > -1 || url.indexOf("buzurl.com") > -1 || url.indexOf("cutt.us") > -1 ||
      url.indexOf("u.bb") > -1 || url.indexOf("yourls.org") > -1 || url.indexOf("crisco.com") > -1 ||
      /*url.indexOf("x.co") > -1 ||*/  url.indexOf("prettylinkpro.com") > -1 || url.indexOf("viralurl.biz") > -1 ||
      url.indexOf("adcraft.co") > -1 || url.indexOf("virl.ws") > -1 || url.indexOf("scrnch.me") > -1 ||
      url.indexOf("filoops.info") > -1 || url.indexOf("vurl.bz") > -1 || url.indexOf("vzturl.com") > -1 ||
      url.indexOf("lemde.fr") > -1 || url.indexOf("qr.net") > -1 || url.indexOf("1url.com") > -1 ||
      url.indexOf("tweez.me") > -1 || url.indexOf("7vd.cn") > -1 || url.indexOf("v.gd") > -1 ||
      url.indexOf("dft.ba") > -1 || url.indexOf("aka.gr") > -1 || url.indexOf("tr.im") > -1) {
      WS.url("http://expandurl.appspot.com/expand")
        .withQueryString("url" -> ("http://" + url))
        .get()
        .map { response =>
          readUnshortResponse(response.json) match {
            case Some(shortedUrl) => shortedUrl
            case _ => url
          }
        } recoverWith {
        case NonFatal(e) =>
          Logger.error("Utilities.unshortLink: for url " + url + "\nMessage:" + e.getMessage)
          Future(url)
      }
    } else
      Future(url)
  }

  def readUnshortResponse(jsonResponse: JsValue): Option[String] = Try {
    val readUnshortedUrl: Reads[Option[String]] = (__ \ "end_url").readNullable[String]

    jsonResponse.asOpt[Option[String]](readUnshortedUrl) match {
      case Some(Some(url)) => Option(normalizeUrl(url))
      case _ => None
    }
  } match {
    case Success(Some(shortedUrl)) =>
      Option(shortedUrl)
    case Success(None) =>
      None
    case Failure(e) =>
      Logger.error("Utilities.readUnshortResponse:\nException:", e)
      None
  }

  def getNormalizedWebsitesInText(description: String): Future[Set[String]] = Future.sequence {
    removeMailFromListOfWebsites(linkPattern.findAllIn(description).toSet) map { webSite =>
      unshortLink(normalizeUrlWithoutLowerCase(webSite)) map { _.toLowerCase }
    }
  }

  def phoneNumbersStringToSet(phoneNumbers: Option[String]): Set[String] = phoneNumbers match {
    case None => Set.empty
    case Some(phoneNumbersValue: String) =>
      def normalizePhoneNumberPrefix(phoneNumber: String): String = phoneNumber match {
        case phoneNumberStartsWith0033 if phoneNumberStartsWith0033.startsWith("0033") =>
          "0" + phoneNumber.drop(4)
        case phoneNumberStartsWith0033 if phoneNumberStartsWith0033.startsWith("+0033") =>
          "0" + phoneNumber.drop(5)
        case phoneNumberStartsWith33 if phoneNumberStartsWith33.startsWith("33") =>
          "0" + phoneNumber.drop(2)
        case phoneNumberStartsWithPlus33 if phoneNumberStartsWithPlus33.startsWith("+33") =>
          "0" + phoneNumber.drop(3)
        case alreadyNormalized: String => alreadyNormalized
        case _ => ""
      }

      var numberWithoutLetters = phoneNumbersValue.replaceAll("[^0-9+]", "")
      var normalizedNumbers = ListBuffer.empty[String]

      while(numberWithoutLetters.length >= 10) {
        val withNormalizedPrefix = normalizePhoneNumberPrefix(numberWithoutLetters)
        normalizedNumbers += withNormalizedPrefix.take(10)
        numberWithoutLetters = withNormalizedPrefix.drop(10)
      }
      normalizedNumbers.toSet.filter(_ == "")
  }

  def phoneNumbersSetToOptionString(phoneNumbers: Set[String]): Option[String] = phoneNumbers match {
    case emptySet: Set[String] if emptySet.isEmpty => None
    case phoneNumbersFound => Option(phoneNumbersFound.mkString(","))
  }

  def formatDescription(description: Option[String]): Option[String] = description match {
    //see if not faster to useGetWebsitesInDescription and after replace all matched ?
    case None =>
      None
    case Some(desc) =>
      val newDesc = desc.replaceAll("""<""", "&lt;").replaceAll( """>""", "&gt;")
      def stringToLinks(matcher: Regex.Match): String = {
        val phoneNumberPattern = """([\d\.]+)""".r
        val matcherString = matcher.toString()
        matcherString match {
          case phoneNumberPattern(link) =>
            matcherString
          case _ =>
            if (matcherString contains "@")
              matcherString
            else
              """<a href='http://""" + normalizeUrl(matcherString) + """'>""" + normalizeUrl(matcherString) +
                """</a>"""
        }
      }
      Option("<div class='column large-12'>" +
        linkPattern.replaceAllIn(newDesc, m => stringToLinks(m))
          .replaceAll( """\n\n""", "<br/><br/></div><div class='column large-12'>")
          .replaceAll( """\n""", "<br/>")
          .replaceAll( """\t""", "    ").trim +
        "</div>")
  }

  def stringToDateTime(string: String): DateTime = {
    val formattedString = string.replace("T", " ").substring(0, string.length - 5)
    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(formattedString)
  }

  def optionStringToOptionDateTime(maybeDate: Option[String]): Option[DateTime] = maybeDate match {
    case Some(date) => Option(stringToDateTime(date))
    case None => None
  }

  def refactorEventOrPlaceName(eventName: String): String = {
    eventName.indexOf(" @") match {
      case -1 => eventName
      case index => eventName.take(index).trim
    }
  }

  def setToOptionString(set: Set[String]): Option[String] = if (set.isEmpty) None else Option(set.mkString(","))
}
