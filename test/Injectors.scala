import models._
import play.api.Configuration
import play.api.db.DBApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}
import silhouette.UserDAOImpl


trait Injectors {
  lazy val appBuilder = new GuiceApplicationBuilder().configure(Configuration.from(Map(
        "slick.dbs.default.driver" -> "slick.driver.PostgresDriver$",
        "slick.dbs.default.db.driver" -> "org.postgresql.Driver",
        "slick.dbs.default.db.url" -> "jdbc:postgresql://localhost:5432/tests",
        "slick.dbs.default.db.user" -> "simon",
        "slick.dbs.default.db.password" -> "root",
        "slick.dbs.default.db.connectionTimeout" -> "5 seconds",
        "slick.dbs.default.db.connectionPool" -> "disabled")))
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
  lazy val eventMethods = new EventMethods(dbConfProvider, organizerMethods, artistMethods, tariffMethods,
    genreMethods, geographicPointMethods, utilities)
  lazy val userDAOImpl = new UserDAOImpl(dbConfProvider)
  lazy val playlistMethods = new PlaylistMethods(dbConfProvider, utilities)

  lazy val databaseApi = injector.instanceOf[DBApi]
}
