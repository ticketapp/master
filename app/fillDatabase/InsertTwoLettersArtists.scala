package fillDatabase

import models._
import play.api.Logger
import play.api.mvc._
import controllers.SearchArtistsController
import play.api.libs.concurrent.Execution.Implicits._


object InsertTwoLettersArtists extends Controller {

  def insertTwoLettersArtists() = Action {
    val chars = "abcdefghijklmnopqrstuvwxyzàçéèëêîïôöùûü0123456789?!$+".toArray
    for (c1 <- chars) {
      for (c2 <- chars) {
        SearchArtistsController.getEventuallyFacebookArtists(c1.toString + c2.toString) map { artists =>
          artists map { Artist.save }
        }
        Thread.sleep(120)
      }
    }
    Ok
  }
}
