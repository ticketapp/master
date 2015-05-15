package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account4686 (id: Long,
                       date: Date,
                       name: Option[String],
                       amount: BigDecimal,
                       debit: Boolean,
                       account63Id: Long)


object Account4686 {
  private val Account4686Parser: RowParser[Account4686] = {
    get[Long]("id") ~
      get[Date]("date") ~
      get[Option[String]]("name") ~
      get[BigDecimal]("amount") ~
      get[Boolean]("debit") ~
      get[Long]("account63Id") map {
      case id ~ date ~ name ~ amount ~ debit ~ account63Id =>
        Account4686.apply(id, date, name, amount, debit, account63Id)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account4686.id, account4686.date, account4686.name, account4686.amount, account4686.debit,
            account4686.account63Id
        FROM account4686
        ORDER BY account4686.date DESC""").as(Account4686Parser *)
    }
  }

  def findAllWithCredit() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account4686.id, account4686.date, account4686.name, account4686.amount, account4686.debit,
            account4686.account63Id
        FROM account4686
        WHERE debit = false
        ORDER BY account4686.date DESC""").as(Account4686Parser *)
    }
  }
}