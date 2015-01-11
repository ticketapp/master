package models

import play.api.libs.json.DefaultWrites
import scala.BigDecimal.javaBigDecimal2bigDecimal
import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.{Json, JsNull, Writes}
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal

case class Account403 (id: Long,
                       date: Date,
                       amount: BigDecimal,
                       debit: Boolean,
                       account60Id: Long)

object Account403 {

  private val Account403Parser: RowParser[Account403] = {
    get[Long]("id") ~
      get[Date]("date") ~
      get[BigDecimal]("amount") ~
      get[Boolean]("debit") ~
      get[Long]("account60Id") map {
      case id ~ date ~ amount ~ debit ~ account60Id =>
        Account403.apply(id, date, amount, debit, account60Id)
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account403.id, account403.date, account403.amount, account403.debit, account403.account60Id
        FROM account403
        ORDER BY account403.date DESC""").as(Account403Parser *)
    }
  }

  def findAllByOrgaIdWithCredit(orgaId: Long) = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT account403.id, account403.date, account403.amount, account403.debit, account403.account60Id
        FROM account403
        WHERE account403.debit = false AND account403.userId = {orgaId}
        ORDER BY account403.date DESC""")
        .on('orgaId -> orgaId)
        .as(Account403Parser *)
    }
  }
}