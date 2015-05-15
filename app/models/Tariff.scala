package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.Play.current
import controllers.{SchedulerException, DAOException}
import java.util.Date
import java.math.BigDecimal

case class Tariff (tariffId: Long,
                   denomination: String,
                   nbTicketToSell: Int,
                   nbTicketSold: Int,
                   price: BigDecimal,
                   startTime: Date,
                   endTime: Date,
                   eventId: Long)

object Tariff {
  def formApply(denomination: String,  nbTicketToSell: Int, price: scala.BigDecimal, startTime: Date, endTime: Date) = {
    println("ok")
    new Tariff(-1L, denomination, nbTicketToSell, 0, price.bigDecimal, startTime, endTime, -1L)
  }
  def formUnapply(tariff: Tariff) = Some((tariff.denomination, tariff.nbTicketToSell, scala.BigDecimal(tariff.price),
                                          tariff.startTime, tariff.endTime))

  private val TariffParser: RowParser[Tariff] = {
    get[Long]("tariffId") ~
    get[String]("denomination") ~
    get[Int]("nbTicketToSell") ~
    get[Int]("nbTicketSold") ~
    get[BigDecimal]("price") ~
    get[Date]("startTime") ~
    get[Date]("endTime") ~
    get[Long]("eventId") map {
      case tariffId ~ denomination ~ nbTicketToSell ~ nbTicketSold ~ price ~ startTime ~ endTime ~ eventId =>
        Tariff(tariffId, denomination, nbTicketToSell, nbTicketSold, price, startTime, endTime, eventId)
    }
  }

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
  }

  def findPrices(description: Option[String]): Option[String] = description match {
    case None =>
      None
    case Some(desc) =>
      try {
        """(\d+[,.]?\d+)\s*€""".r.findAllIn(desc).matchData.map { priceMatcher =>
          priceMatcher.group(1).replace(",", ".").toFloat
        }.toList match {
          case list: List[Float] if list.isEmpty => None
          case prices => Option(prices.min.toString + "-" + prices.max.toString)
        }
      } catch {
        case e: Exception => throw new Exception("Tarrif.findPrices: " + e.getMessage)
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
