package genres

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Genre(id: Option[Int] = None, name: String, icon: Char)

@JSExportAll
case class GenreWithWeight(genre: Genre, weight: Int)
