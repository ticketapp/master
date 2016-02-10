package tariffsDomain

import javax.inject.Inject

import database.{MyDBTableDefinitions, MyPostgresDriver}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.Future
import MyPostgresDriver.api._

case class Tariff (tariffId: Option[Long] = None,
                    denomination: String,
                   eventId: Long,
                   startTime: DateTime,
                   endTime: DateTime,
                   price: BigDecimal)

class TariffMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def save(tariff: Tariff): Future[Long] = db.run(tariffs returning tariffs.map(_.tariffId) += tariff)

  def findByEventId(eventId: Long): Future[Seq[Tariff]] = db.run(tariffs.filter(_.eventId === eventId).result)
  
  /*def formApply(denomination: String,  nbTicketToSell: Int, price: scala.BigDecimal, startTime: Date, endTime: Date) =
    new Tariff(-1L, denomination, nbTicketToSell, 0, price.bigDecimal, startTime, endTime, -1L)
  def formUnapply(tariff: Tariff) = Some((tariff.denomination, tariff.nbTicketToSell, scala.BigDecimal(tariff.price),
                                          tariff.startTime, tariff.endTime))*/

  def findPricesInDescription(description: Option[String]): Option[String] = description match {
    case None =>
      None
    case Some(descriptionFound) =>
      try {
        val descriptionWithoutComas = descriptionFound.replace(",", ".")
        val tariffPattern = """(\d+|\d+\.?\d*)\s*€""".r
        val tariffs: Set[Float] = tariffPattern.findAllIn(descriptionWithoutComas).matchData.map { priceMatcher =>
          priceMatcher.group(1).toFloat
        }.toSet
        tariffs match {
          case emptySet if emptySet.isEmpty => None
          case prices => Option(prices.min.toString + "-" + prices.max.toString)
        }
      } catch {
        case e: Exception => throw new Exception("Tariff.findPrices: " + e.getMessage)
      }
  }

  def findTicketSellers(normalizedWebsites: Set[String]): Option[String] = {
    val websites = normalizedWebsites.filter(website =>
      website.contains("digitick") && website != "digitick.com" ||
        website.contains("weezevent") && website != "weezevent.com" ||
        website.contains("yurplan") && website != "yurplan.com" ||
        website.contains("eventbrite") && website != "eventbrite.fr" ||
        website.contains("ticketmaster") && website != "ticketmaster.fr" ||
        website.contains("fnacspectacles") && website != "fnacspectacles.com" ||
        website.contains("ticketnet") && website != "ticketnet.fr")

    if(websites.isEmpty) None
    else Option(websites.mkString(","))
  }
}
