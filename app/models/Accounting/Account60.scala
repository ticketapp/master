package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account60 (id: Long,
                       datePayment: Date,
                       name: String,
                       amount: BigDecimal,
                       paymentReference: Option[Int],
                       orderId: Long)

object Account60 {
  private val Account60Parser: RowParser[Account60] = {
    get[Long]("id") ~
      get[Date]("datePayment") ~
      get[String]("name") ~
      get[BigDecimal]("amount") ~
      get[Option[Int]]("paymentReference") ~
      get[Long]("orderId") map {
      case id ~ datePayment ~ name ~ amount ~ paymentReference ~ orderId =>
        Account60.apply(id, datePayment, name, amount, paymentReference, orderId)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account60.id, account60.datePayment, account60.name, account60.amount,
            account60.paymentReference, account60.orderId
        FROM account60
        ORDER BY account60.datePayment DESC""").as(Account60Parser *)
    }
  }
}