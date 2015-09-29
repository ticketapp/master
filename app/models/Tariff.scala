package models


import javax.inject.Inject

import play.api.Play.current
import controllers.{SchedulerException, DAOException}
import java.util.Date
import java.math.BigDecimal

import play.api.db.slick.DatabaseConfigProvider
import services.Utilities
import slick.driver.JdbcProfile

case class Tariff (tariffId: Long,
                   denomination: String,
                   nbTicketToSell: Int,
                   nbTicketSold: Int,
                   price: BigDecimal,
                   startTime: Date,
                   endTime: Date,
                   eventId: Long)

class TariffMethods @Inject()(dbConfigProvider: DatabaseConfigProvider,
                             val utilities: Utilities) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  def formApply(denomination: String,  nbTicketToSell: Int, price: scala.BigDecimal, startTime: Date, endTime: Date) =
    new Tariff(-1L, denomination, nbTicketToSell, 0, price.bigDecimal, startTime, endTime, -1L)
  def formUnapply(tariff: Tariff) = Some((tariff.denomination, tariff.nbTicketToSell, scala.BigDecimal(tariff.price),
                                          tariff.startTime, tariff.endTime))

/*
  def findAll(): List[Tariff] = {
    DB.withConnection { implicit connection =>
      SQL("select * from tariffs").as(TariffParser.*)
    }
  }

  def find(tariffId: Long): Option[Tariff] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from tariffs WHERE tariffId = {tariffId}")
        .on('tariffId -> tariffId)
        .as(TariffParser.singleOpt)
    }
  }

  def findAllByEvent(event: Event): List[Tariff] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM Tariffs
             WHERE eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(TariffParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Tariff.findAllByEvent: " + e.getMessage)
  }

  def save(tariff: Tariff) = try {
    DB.withConnection { implicit connection =>
      SQL( """INSERT INTO tariffs (denomination, nbTicketToSell, price, startTime, endTime, eventId)
          VALUES ({denomination}, {nbTicketToSell}, {price}, {startTime}, {endTime}, {eventId})""")
        .on(
          'denomination -> tariff.denomination,
          'nbTicketToSell -> tariff.nbTicketToSell,
          'price -> tariff.price,
          'startTime -> tariff.startTime,
          'endTime -> tariff.endTime,
          'eventId -> tariff.eventId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Tariff.save: " + e.getMessage)
  }*/

  def findPrices(description: Option[String]): Option[String] = description match {
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
    normalizedWebsites.filter(website =>
      website.contains("digitick") && website != "digitick.com" ||
        website.contains("weezevent") && website != "weezevent.com" ||
        website.contains("yurplan") && website != "yurplan.com" ||
        website.contains("eventbrite") && website != "eventbrite.fr" ||
        website.contains("ticketmaster") && website != "ticketmaster.fr" ||
        website.contains("ticketnet") && website != "ticketnet.fr")
    match {
      case set: Set[String] if set.isEmpty => None
      case websites: Set[String] => Option(websites.mkString(","))
    }
  }
}
