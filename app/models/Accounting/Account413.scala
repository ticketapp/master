package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account413 (id: Long,
                       clientId: Long,
                       date: Date,
                       amount: BigDecimal,
                       debit: Boolean)

object Account413 {
  private val Account413Parser: RowParser[Account413] = {
    get[Long]("id") ~
    get[Long]("clientId") ~
    get[Date]("date") ~
    get[BigDecimal]("amount") ~
    get[Boolean]("debit") map {
      case id ~ clientId ~ date ~ amount ~ debit =>
        Account413.apply(id, clientId, date, amount, debit)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account413.id, account413.clientId, account413.date, account413.amount, account413.debit
        FROM account413
        ORDER BY account413.date DESC""").as(Account413Parser *)
    }
  }
}