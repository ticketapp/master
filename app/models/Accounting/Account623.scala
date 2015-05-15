package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account623 (id: Long,
                       datePayment: Date,
                       amount: BigDecimal,
                       name: String,
                       billId: Option[Long])

object Account623 {
  private val Account623Parser: RowParser[Account623] = {
    get[Long]("id") ~
      get[Date]("datePayment") ~
      get[BigDecimal]("amount") ~
      get[String]("name") ~
      get[Option[Long]]("billId") map {
      case id ~ datePayment ~ amount ~ name ~ billId =>
        Account623.apply(id, datePayment, amount, name, billId)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account623.id, account623.datePayment, account623.amount, account623.name, account623.billId
        FROM account623
        ORDER BY account623.datePayment DESC""").as(Account623Parser *)
    }
  }
}