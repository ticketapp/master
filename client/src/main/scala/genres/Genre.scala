package genres

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Genre(id: Option[Int], name: String, icon: Char)
case class GenreWithWeight(genre: Genre, weight: Int)
