package fillDatabase

import models.Artist
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._


class InsertTwoLettersArtists extends Controller {

  def insertTwoLettersArtists() = Action {
    val chars = "abcdefghijklmnopqrstuvwxyzàçéèëêîïôöùûü0123456789?!$+".toArray
//    for (c1 <- chars) {
//      for (c2 <- chars) {
//        Artist.getEventuallyFacebookArtists(c1.toString + c2.toString) map { artists =>
//          artists map { Artist.save }
//        }
//        Thread.sleep(120)
//      }
//    }
    Ok
  }
}
