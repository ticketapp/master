package controllers

import anorm._
import models._
import json.JsonHelper._
import play.api.db.DB
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import play.api.Play.current

object AccountingCtrlr extends Controller {
  def createBankLine(amount: BigDecimal, debit: Boolean, paymentReference: Option[Int], orderId: Long) = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into bank(amount, debit, orderId) values ({amount}, {debit}, {orderId})").on(
          'amount -> amount,
          'debit -> debit,
          'orderId -> orderId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create bank line: " + e.getMessage)
    }
  }

  def createAccount63Line(name: String, amount: BigDecimal, orderId: Long) = {
    val amountTVA = amount * 5.5 / 100
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into account63(name, amount, orderId) values ({name}, {amountTVA}, {orderId})").on(
          'name -> name,
          'amountTVA -> amountTVA,
          'orderId -> orderId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create a new line on account 63: " + e.getMessage)
    }
  }


  def createAccount627Line(name: String, bankAccountCharge: BigDecimal, orderId: Long) = {
    //val bankAccountCharge = amount * 1/100
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into account627(name, amount, orderId) values ({name}, {bankAccountCharge}, {orderId})").on(
          'name -> name,
          'bankAccountCharge -> bankAccountCharge,
          'orderId -> orderId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create a new line on account 627: " + e.getMessage)
    }
  }

  def createAccount627LineAndBankLine(name: String, amount: BigDecimal, orderId: Long) = {
    val bankAccountCharge = amount * 1/100
    val account627Id = createAccount627Line(name, bankAccountCharge, orderId)
    try {
      DB.withConnection { implicit connection =>
        SQL( """insert into bank(name, amount, debit, account627Id)
                values ({name}, {bankAccountCharge}, false, {account627Id})""").on(
            'name -> name,
            'bankAccountCharge -> bankAccountCharge,
            'account627Id -> account627Id
          ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create a new line on account 627: " + e.getMessage)
    }
  }

  def createAccount4686Line(debit: Boolean, amount: BigDecimal,account63Id: BigInt) = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into account4686(debit, amount, account63Id) values ({debit}, {amount}, {account63Id})").on(
          'debit -> debit,
          'amount -> amount,
          'account63Id -> account63Id
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create a new line on account 4686: " + e.getMessage)
    }
  }

  def createAccount60Line(name: String, orgaReturn: BigDecimal, orderId: BigInt) = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into account60(name, amount, orderId) values ({name}, {orgaReturn}, {orderId})").on(
          'name -> name,
          'orgaReturn -> orgaReturn,
          'orderId -> orderId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create a new line on account 60: " + e.getMessage)
    }
  }

  def createAccount60LineAndAccount403Line(name: String, amount: BigDecimal, orderId: BigInt) = {
    val orgaReturn = amount * 95/100
    val account60Id = createAccount60Line(name, orgaReturn, orderId)
    try {
      DB.withConnection { implicit connection =>
        SQL( """insert into account403(amount, debit, account60Id)
                values ({orgaReturn}, false, {account60Id})""").on(
            'orgaReturn -> orgaReturn,
            'account60Id -> account60Id
          ).executeInsert().get
      }
    } catch {
      case e: Exception =>
        throw new DAOException("Cannot create a new line on account 403 (after created account60line): " + e.getMessage)
    }
  }


  def orgaPayment = Action {// tous les 403 crédit de lorga d'id orgaId
    //Ok(Json.toJson(Account403.findAll()))
    Ok(Json.toJson(Account403.findAllByOrgaIdWithCredit(1)))
  }


  def totalToPay = Action { //total amount credit 403 et 4686
    Ok(JsArray(Seq(Json.toJson(Account4686.findAllWithCredit()), Json.toJson(Account403.findAllByOrgaIdWithCredit(1)))))
  }

  def account708 = Action {
    Ok(Json.toJson(Account708.findAll()))
  }


  def account413 = Action {
    Ok(Json.toJson(Account413.findAll()))
  }

  //toutes les charges : 60, 63, 627, 623, 626
  def account60 = Action {
    Ok(Json.toJson(Account60.findAll()))
  }


  def account63 = Action {
    Ok(Json.toJson(Account63.findAll()))
  }


  def account627 = Action {
    Ok(Json.toJson(Account627.findAll()))
  }

  def account623 = Action {
    Ok(Json.toJson(Account623.findAll()))
  }

  def account626 = Action {
    Ok(Json.toJson(Account626.findAll()))
  }
}