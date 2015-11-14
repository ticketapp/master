package models

import play.api.data.Form
import play.api.data.Forms._

import scala.collection.immutable.Seq


trait artistFormsTrait extends genreFormsTrait {

  val artistBindingForm = Form(
    mapping(
      "searchPattern" -> nonEmptyText(3),
      "artist" -> mapping(
        "facebookId" -> optional(nonEmptyText(2)),
        "artistName" -> nonEmptyText(2),
        "imagePath" -> optional(nonEmptyText(2)),
        "description" -> optional(nonEmptyText),
        "facebookUrl" -> nonEmptyText,
        "websites" -> seq(nonEmptyText(4)),
        "genres" -> seq(genreWithWeightBindingForm),
        "likes" -> optional(number),
        "country" -> optional(nonEmptyText)
      )(artistFormApply)(artistFormUnapply)
    )(formWithPatternApply)(formWithPatternUnapply)
  )

  def artistFormApply(facebookId: Option[String], name: String, imagePath: Option[String], description: Option[String],
                facebookUrl: String, websites: scala.Seq[String], genres: scala.Seq[GenreWithWeight], likes: Option[Int],
                country: Option[String]): ArtistWithWeightedGenresAndHasTrack =
    ArtistWithWeightedGenresAndHasTrack(
      Artist(None, facebookId, name, imagePath, description, facebookUrl, websites.toSet, likes, country),
      genres.toVector)

  def artistFormUnapply(artistWithWeightedGenre: ArtistWithWeightedGenresAndHasTrack) = {
    val artist = artistWithWeightedGenre.artist
    Option((artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
      artist.websites.toSeq, artistWithWeightedGenre.genres, artist.likes, artist.country))

  }

  def formWithPatternApply(searchPattern: String, artist: ArtistWithWeightedGenresAndHasTrack) =
    PatternAndArtist(searchPattern, artist)

  def formWithPatternUnapply(searchPatternAndArtist: PatternAndArtist) =
    Option((searchPatternAndArtist.searchPattern, searchPatternAndArtist.artistWithWeightedGenre))
}
