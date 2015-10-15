/*
package models

<<<<<<< HEAD


=======
import anorm.SqlParser._
import anorm._
>>>>>>> master

import play.api.libs.json.Json
import play.api.Play.current
import controllers.DAOException
import scala.language.postfixOps

case class Order (orderId: Long,
                  totalPrice: Int)


object Order {
  implicit val orderWrites = Json.writes[Order]

  private val OrderParser: RowParser[Order] = {
    get[Long]("id") ~
      get[Int]("totalPrice") map {
      case id ~ totalPrice =>
        Order(id, totalPrice)
    }
  }

  def findAll(): Seq[Order] = {
    DB.withConnection { implicit connection =>
      SQL("select * from orders").as(OrderParser *)
    }
  }

  def find(id: Long): Option[Order] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from orders WHERE id = {id}")
        .on('id -> id)
        .as(OrderParser.singleOpt)
    }
  }

  def save(totalPrice: Int): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """
            INSERT INTO orders(totalPrice)
            VALUES({totalPrice})
          """).on(
          'totalPrice ->
            totalPrice
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save order: " + e.getMessage)
    }
  }

}
*/
