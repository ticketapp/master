package genresDomain

import play.api.data.Forms._


trait genreFormsTrait {

  val genreWithWeightBindingForm = mapping(
    "name" -> nonEmptyText,
    "weight" -> number
  )(formApply)(formUnapply)

  def formApply(name: String, weight: Int) = new GenreWithWeight(Genre(None, name), weight)
  def formUnapply(genreWithWeight: GenreWithWeight) = Some((genreWithWeight.genre.name, genreWithWeight.weight))
}
