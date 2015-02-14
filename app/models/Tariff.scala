package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.Play.current
import controllers.DAOException
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
  def formApply(denomination: String,  nbTicketToSell: Int, price: scala.BigDecimal, startTime: Date, endTime: Date)
    = {
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

  def findAll(): Seq[Tariff] = {
    DB.withConnection { implicit connection =>
      SQL("select * from tariffs").as(TariffParser *)
    }
  }

  def find(tariffId: Long): Option[Tariff] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from tariffs WHERE tariffId = {tariffId}")
        .on('tariffId -> tariffId)
        .as(TariffParser.singleOpt)
    }
  }

  def findAllByEvent(event: Event): Seq[Tariff] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM Tariffs
             WHERE eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(TariffParser *)
    }
  }

  def save(tariff: Tariff) = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO tariffs (denomination, nbTicketToSell, price, startTime, endTime, eventId)
            VALUES ({denomination}, {nbTicketToSell}, {price}, {startTime}, {endTime}, {eventId})""").on(
            'denomination -> tariff.denomination,
            'nbTicketToSell -> tariff.nbTicketToSell,
            'price -> tariff.price,
            'startTime -> tariff.startTime,
            'endTime -> tariff.endTime,
            'eventId -> tariff.eventId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save tariff: " + e.getMessage)
    }
  }
}
