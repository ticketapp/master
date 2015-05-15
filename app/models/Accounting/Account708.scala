package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account708 (id: Long,
                       date: Date,
                       name: String,
                       amount: BigDecimal,
                       clientId: Long,
                       orderId: Long)

object Account708 {
  private val Account708Parser: RowParser[Account708] = {
    get[Long]("id") ~
      get[Date]("date") ~
      get[String]("name") ~
      get[BigDecimal]("amount") ~
      get[Long]("clientId") ~
      get[Long]("orderId") map {
      case id ~ date ~ name ~ amount ~ clientId ~ orderId =>
        Account708.apply(id, date, name, amount, clientId, orderId)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account708.id, account708.date, account708.name, account708.amount, account708.clientId,
            account708.orderId
        FROM account708
        ORDER BY account708.date DESC""").as(Account708Parser *)
    }
  }
}