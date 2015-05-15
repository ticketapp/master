package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account627 (id: Long,
                       datePayment: Date,
                       amount: BigDecimal,
                       name: String,
                       account60Id: Option[Long])

object Account627 {
  private val Account627Parser: RowParser[Account627] = {
    get[Long]("id") ~
      get[Date]("datePayment") ~
      get[BigDecimal]("amount") ~
      get[String]("name") ~
      get[Option[Long]]("orderId") map {
      case id ~ datePayment ~ amount ~ name ~ account60Id =>
        Account627.apply(id, datePayment, amount, name, account60Id)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account627.id, account627.datePayment, account627.amount, account627.name, account627.orderId
        FROM account627
        ORDER BY account627.datePayment DESC""").as(Account627Parser *)
    }
  }
}