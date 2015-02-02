package jobs

import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits._
import scala.io.Source

object savePlacesFromTextFiles {
  val fileLines = Source.fromFile("textFiles/400to1200PlacesStructures.txt").getLines.toList
  fileLines.foreach(println)

}
