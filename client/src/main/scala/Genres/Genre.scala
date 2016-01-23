package Genres

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Genre (id: Option[Int], name: String, icon: Char = 'a') {
  require(name.nonEmpty, "It is forbidden to create a genre without a name.")
}

case class GenreWithWeight(genre: Genre, weight: Int)
