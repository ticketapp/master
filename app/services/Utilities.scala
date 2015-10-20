package services

import java.sql.{Timestamp, Connection}
import java.text.Normalizer
import java.util.{UUID, Date}
import javax.inject.Inject

import org.joda.time.DateTime
import play.api.Logger
import slick.driver.PostgresDriver.api._

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import scala.util.{Try, Failure, Success}
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point}


class Utilities @Inject()() {
  val facebookToken = "1434769156813731%7Cf2378aa93c7174712b63a24eff4cb22c"
  val googleKey = "AIzaSyDx-k7jA4V-71I90xHOXiILW3HHL0tkBYc"
  val echonestApiKey = "3ZYZKU3H3MKR2M59Z"
  val soundCloudClientId = "f297807e1780623645f8f858637d4abb"

  val linkPattern = """((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-z@A-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)""".r

  val UNIQUE_VIOLATION = "23505"
  val FOREIGN_KEY_VIOLATION = "23503"
  val facebookApiVersion = "v2.4"

  implicit def dateTime = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime))

  def normalizeString(string: String): String = string //Should be replace accentued letters for example?

  def replaceAccentuatedLetters(string: String): String =
    Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")

  def stripChars(s:String, ch:String)= s filterNot (ch contains _)

  def normalizeUrl(website: String): String =
    """(https?:\/\/(www\.)?)|(www\.)""".r.replaceAllIn(website.toLowerCase, p => "").stripSuffix("/")


  def optionStringToLowerCaseOptionString(maybeString: Option[String]): Option[String] = maybeString match {
    case Some(string: String) => Option(string.toLowerCase)
    case None => None
  }

  def removeMailFromListOfWebsites(websites: Set[String]): Set[String] = websites.filter(website =>
    website.indexOf("@") == -1)

  def removeSpecialCharacters(string: String): String = string.replaceAll("""[*ù$-+/*_\.\\,#'~´&]""", "")

  def getNormalizedWebsitesInText(maybeDescription: Option[String]): Set[String] = maybeDescription match {
    case None =>
      Set.empty
    case Some(description) =>
      removeMailFromListOfWebsites(linkPattern.findAllIn(description).toSet).map { normalizeUrl }
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
      }

      var numberWithoutLetters = phoneNumbersValue.replaceAll("[^0-9+]", "")
      var normalizedNumbers = ListBuffer.empty[String]

      while (numberWithoutLetters.length >= 10) {
        val withNormalizedPrefix = normalizePhoneNumberPrefix(numberWithoutLetters)
        normalizedNumbers += withNormalizedPrefix.take(10)
        numberWithoutLetters = withNormalizedPrefix.drop(10)
      }
      normalizedNumbers.toSet
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

  def formatDate(date: Option[String]): Option[Date] = date match {
    case Some(dateFound: String) => val date = dateFound.replace("T", " ")
      date.length match {
        case i if i <= 10 => Option(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(date))
        case i if i <= 13 => Option(new java.text.SimpleDateFormat("yyyy-MM-dd HH").parse(date))
        case _ => Option(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date))
      }
    case _ => None
  }

  def refactorEventOrPlaceName(eventName: String): String = {
    eventName.indexOf(" @") match {
      case -1 => eventName
      case index => eventName.take(index).trim
    }
  }

  def setToOptionString(set: Set[String]): Option[String] =
    if (set.isEmpty) None else Option(set.mkString(","))
}
