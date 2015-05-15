package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account626 (id: Long,
                       datePayment: Date,
                       amount: BigDecimal,
                       name: String,
                       paymentReference: Int)

object Account626 {
  private val Account626Parser: RowParser[Account626] = {
    get[Long]("id") ~
      get[Date]("datePayment") ~
      get[BigDecimal]("amount") ~
      get[String]("name") ~
      get[Int]("paymentReference") map {
      case id ~ datePayment ~ amount ~ name ~ paymentReference =>
        Account626.apply(id, datePayment, amount, name, paymentReference)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account626.id, account626.datePayment, account626.amount, account626.name,
            account626.paymentReference
        FROM account626
        ORDER BY account626.datePayment DESC""").as(Account626Parser *)
    }
  }
}