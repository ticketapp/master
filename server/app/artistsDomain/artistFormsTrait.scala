package artistsDomain

import genresDomain.{GenreWithWeight, genreFormsTrait}
import play.api.data.{Mapping, Form}
import play.api.data.Forms._


trait artistFormsTrait extends genreFormsTrait {

  private val mappingArtist: Mapping[ArtistWithWeightedGenres] = mapping(
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

  val artistBindingForm = Form(mappingArtist)

  val artistWithPatternBindingForm = Form(
    mapping(
      "searchPattern" -> nonEmptyText(3),
      "artist" -> mappingArtist
    )(formWithPatternApply)(formWithPatternUnapply)
  )

  def artistFormApply(facebookId: Option[String], name: String, imagePath: Option[String], description: Option[String],
                facebookUrl: String, websites: scala.Seq[String], genres: scala.Seq[GenreWithWeight], likes: Option[Int],
                country: Option[String]): ArtistWithWeightedGenres =
    ArtistWithWeightedGenres(
      Artist(
        id = None,
        facebookId = facebookId,
        name = name,
        imagePath = imagePath,
        description = description,
        facebookUrl = facebookUrl,
        websites = websites.toSet,
        hasTracks = false,
        likes = likes,
        country = country),
      genres.toVector)

  def artistFormUnapply(artistWithWeightedGenre: ArtistWithWeightedGenres) = {
    val artist = artistWithWeightedGenre.artist
    Option((artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
      artist.websites.toSeq, artistWithWeightedGenre.genres, artist.likes, artist.country))

  }

  def formWithPatternApply(searchPattern: String, artist: ArtistWithWeightedGenres) =
    PatternAndArtist(searchPattern, artist)

  def formWithPatternUnapply(searchPatternAndArtist: PatternAndArtist) =
    Option((searchPatternAndArtist.searchPattern, searchPatternAndArtist.artistWithWeightedGenres))
}
