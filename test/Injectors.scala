import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}
import silhouette.UserDAOImpl


trait Injectors {
  lazy val appBuilder = new GuiceApplicationBuilder()
  lazy val injector = appBuilder.injector()
  lazy val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  lazy val utilities = new Utilities()
  lazy val trackMethods = new TrackMethods(dbConfProvider, utilities)
  lazy val genreMethods = new GenreMethods(dbConfProvider, utilities)
  lazy val searchSoundCloudTracks = new SearchSoundCloudTracks(utilities, trackMethods, genreMethods)
  lazy val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  lazy val geographicPointMethods = new SearchGeographicPoint(dbConfProvider, utilities)
  lazy val tariffMethods = new TariffMethods(dbConfProvider, utilities)
  lazy val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  lazy val addressMethods = new AddressMethods(dbConfProvider, utilities, geographicPointMethods)
  lazy val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, addressMethods, utilities, geographicPointMethods)
  lazy val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  lazy val eventMethods = new EventMethods(dbConfProvider, organizerMethods, placeMethods, artistMethods, tariffMethods,
    geographicPointMethods, utilities)
  lazy val userDAOImpl = new UserDAOImpl(dbConfProvider)
  lazy val playlistMethods = new PlaylistMethods(dbConfProvider, utilities)
}
