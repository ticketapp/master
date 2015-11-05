package models

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._


trait trackFormsTrait {
  /*"tracks" -> seq(
           mapping(
             "trackId" -> nonEmptyText(8),
             "title" -> nonEmptyText,
             "url" -> nonEmptyText,
             "platform" -> nonEmptyText,
             "thumbnail" -> optional(nonEmptyText),
             "avatarUrl" -> optional(nonEmptyText),
             "artistName" -> nonEmptyText(2),
             "artistFacebookUrl" -> nonEmptyText(2),
             "redirectUrl" -> optional(nonEmptyText(2))
           )(Track.formApplyForTrackCreatedWithArtist)(Track.formUnapplyForTrackCreatedWithArtist)
         ),*/

  def formApplyForTrackCreatedWithArtist(trackId: String, title: String, url: String, platform: String,
                                         thumbnailUrl: Option[String], userThumbnailUrl: Option[String],
                                         artistFacebookUrl: String, artistName: String, redirectUrl: Option[String]): Track = {
    thumbnailUrl match {
      case Some(thumbnail: String) =>
        Track(UUID.fromString(trackId), title, url, platform(0), thumbnail, artistFacebookUrl, artistName, redirectUrl)
      case None => userThumbnailUrl match {
        case Some(userThumbnail: String) =>
          new Track(UUID.fromString(trackId), title, url, platform(0), userThumbnail, artistFacebookUrl, artistName, redirectUrl)
        case None =>
          throw new Exception("A track must have a thumbnail or a user Thumbnail url to be saved")
      }
    }
  }

  def formUnapplyForTrackCreatedWithArtist(track: Track) = Some((track.uuid.toString, track.title, track.url,
    track.platform.toString, Some(track.thumbnailUrl), None, track.artistFacebookUrl, track.artistName: String,
    track.redirectUrl))

  def formApply(trackId: String, title: String, url: String, platform: String, thumbnailUrl: String,
                artistFacebookUrl: String, artistName: String, redirectUrl: Option[String]): Track =
    new Track(UUID.fromString(trackId), title, url, platform(0), thumbnailUrl, artistFacebookUrl, artistName, redirectUrl)
  def formUnapply(track: Track) =
    Some((track.uuid.toString, track.title, track.url, track.platform.toString, track.thumbnailUrl,
      track.artistFacebookUrl, track.artistName, track.redirectUrl))

  val trackBindingForm = Form(mapping(
    "trackId" -> nonEmptyText(8),
    "title" -> nonEmptyText(2),
    "url" -> nonEmptyText(3),
    "platform" -> nonEmptyText,
    "thumbnailUrl" -> nonEmptyText(2),
    "artistFacebookUrl" -> nonEmptyText(2),
    "artistName" -> nonEmptyText(2),
    "redirectUrl" -> optional(nonEmptyText(2))
  )(formApply)(formUnapply))

  val trackRatingBindingForm = Form(mapping(
    "trackId" -> nonEmptyText(8),
    "rating" -> number,
    "reason" -> optional(nonEmptyText)
  )(trackRatingFormApply)(trackRatingFormUnapply))

  case class TrackRating(trackId: String, rating: Int, reason: Option[Char])

  def trackRatingFormApply(trackId: String, rating: Int, reason: Option[String]): TrackRating =
    new TrackRating(trackId, rating, reason match { case None => None; case Some(string) => Option(string(0)) } )
  def trackRatingFormUnapply(trackRating: TrackRating) =
    Some((trackRating.trackId, trackRating.rating,
      trackRating.reason match { case None => None; case Some(char) => Option(char.toString) }))
}
