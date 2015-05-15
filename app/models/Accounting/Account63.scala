package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account63 (id: Long,
                       datePayment: Date,
                       name: String,
                       amount: BigDecimal,
                       orderId: Option[Long],
                       account708Id: Option[Long])

object Account63 {
  private val Account63Parser: RowParser[Account63] = {
    get[Long]("id") ~
      get[Date]("datePayment") ~
      get[String]("name") ~
      get[BigDecimal]("amount") ~
      get[Option[Long]]("orderId") ~
      get[Option[Long]]("account708Id") map {
      case id ~ datePayment ~ name ~ amount ~ orderId ~ account708Id =>
        Account63.apply(id, datePayment, name, amount, orderId, account708Id)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account63.id, account63.datePayment, account63.name, account63.amount, account63.orderId,
            account63.account708Id
        FROM account63
        ORDER BY account63.datePayment DESC""").as(Account63Parser *)
    }
  }
}