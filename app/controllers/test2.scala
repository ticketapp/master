package controllers


import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._




object Teest extends Controller{
  def test = Action {

    Ok("jlk")
  }
}