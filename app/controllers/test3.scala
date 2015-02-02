package controllers


import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.io.Source
import java.io.{FileReader, FileNotFoundException, IOException}
import scala.util.control.Breaks._


object Test3 extends Controller{
  val token = "CAACEdEose0cBAJSBFZADAQHTW5WmdZB5by7gHXAlGZCxPZC9FMfDTLRoccMeKEAFVUDSZAve3gOV9ipTkkw8kODY3SsG3fVIDQc2S74OMKWGww565iq6CSATdpF9ZBZCZBWCNNtbiXetKB7fD3AXRnZCTKpCBN4GRQ30twAMYZAz5MZCMJfUE55ZCSj7teSloDm9oygjowpJq78Fs4U6A8mUReya2ff9ro2jWj0ZD"

  def returnListOfIdsFromPlaces(resp : play.api.libs.ws.Response): List[String] = {
    var ids: List[String] = List()
    val responseData = resp.json \\ "data"

    var i = 0
    /*for (jsonObject <- responseData(0)) {
      println(jsonObject)
    }*/
    println(responseData(0)(100))

    val responseDataCategory_list = resp.json \ "data" \\ "category_list"
    var indexes: List[Int] = List()
    val listCategoryWeWantToKeep = List(JsString("179943432047564"), JsString("299714525147")) //concert venue, club

    for(j <- 0 until responseDataCategory_list.length) {
      if (listCategoryWeWantToKeep.contains((responseDataCategory_list(j) \\ "id")(0))) {
        indexes = indexes :+ j
        //println((responseDataCategory_list(j) \\ "id")(0))
      }
    }

    indexes.foreach{ x =>
      ids = ids :+ Json.stringify(responseData(x) \ "id").replaceAll("\"", "")
    }
    ids
  }


  def test3 = Action {
    try {
      val fileLines = Source.fromFile("textFiles/400to1200PlacesStructures.txt").getLines.toList

      for (i <- 0 until fileLines.length) {
        if (fileLines(i) == "Salles de 400 à 1200 places") {
          //println(fileLines(i - 1))
          WS.url("https://graph.facebook.com/v2.2/search?q=" + fileLines(i - 1).replace(" ", "+") +
            "&limit=200&type=page&access_token=" + token).get.map( response =>
              returnListOfIdsFromPlaces(response)
            )
        }


      }
    } catch {
      case e: FileNotFoundException => println("Couldn't find that file.")
      case e: IOException => println("Had an IOException trying to read that file")
    }

    Ok("jn")
  }
}
