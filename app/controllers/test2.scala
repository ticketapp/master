package controllers


import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global


object Teest extends Controller{
  def returnListOfIdsFromResponse(resp : play.api.libs.ws.Response): List[String] = {
    var ids: List[String] = List()
    val responseData: JsValue = (resp.json \ "data")
    val responseDataCategory_list = (responseData \\ "category_list" )
    var indexes: List[Int] = List()
    var listCategoryWeWantToKeep = List("179943432047564")

    for(j <- 0 until responseDataCategory_list.length) {
      if (listCategoryWeWantToKeep.contains((responseDataCategory_list(j) \\ "id")(0)))
        indexes = indexes :+ j
    }

    indexes.foreach{ x =>
      ids = ids :+ Json.stringify((responseData(x) \ "id")).replaceAll("\"", "")
    }
    ids
  }


  def returnIdsOfPlace(placeName: String) = {
    for {
      respA <- WS.url("https://graph.facebook.com/v2.2/search?q=" + placeName.replace(" ", "+") + "&limit=200&type=page&access_token=" +
        "CAACEdEose0cBAAdSr4yRU34JjbuJvXXn4GSm2mRBcfJqXOo6VHYIWq8R7ngjUepTKgkNeoO4doHZB7XhX4dMynqZCWhwZAM4bZAHN499msoWpJA125FZBZAkRdXAw1MZBo1TtzCF7KjsOAXVxM1C2uQSZAfS4cIXgaNRbbyA7wqqRZCBQVZBZB7yiNTinbmFiEoZAzoZCtjoJ7wHZCWD8lVUIzqEYMuwLnnshju1UZD")
      .get
      ids: List[String] = returnListOfIdsFromResponse(respA)
      _ = println(ids(0))

      respB <- Future.sequence(ids.map(id =>
        WS.url("https://graph.facebook.com/v2.2/" + id + "/?access_token=" +
          "CAACEdEose0cBAAdSr4yRU34JjbuJvXXn4GSm2mRBcfJqXOo6VHYIWq8R7ngjUepTKgkNeoO4doHZB7XhX4dMynqZCWhwZAM4bZAHN499msoWpJA125FZBZAkRdXAw1MZBo1TtzCF7KjsOAXVxM1C2uQSZAfS4cIXgaNRbbyA7wqqRZCBQVZBZB7yiNTinbmFiEoZAzoZCtjoJ7wHZCWD8lVUIzqEYMuwLnnshju1UZD")
        .get))

    } yield {
      println(respA.json)

      respB.map(response => println(response.json))

    }
  }


  def test = Action {
      returnIdsOfPlace("Le Transbordeur")

      Ok("qsdqsdqs")
  }
}