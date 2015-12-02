package testsHelper

import actors.DuplicateTracksActorInstance
import addresses.{AddressMethods, SearchGeographicPoint}
import akka.actor.ActorSystem
import artistsDomain.ArtistMethods
import eventsDomain.EventMethods
import genresDomain.GenreMethods
import issues.IssueMethods
import organizersDomain.OrganizerMethods
import others.TariffMethods
import placesDomain.PlaceMethods
import play.api.Configuration
import play.api.db.DBApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import playlistsDomain.PlaylistMethods
import services.GetUserLikedPagesOnFacebook
import silhouette.{OAuth2InfoDAO, UserDAOImpl}
import tracksDomain.{SearchSoundCloudTracks, SearchYoutubeTracks, TrackMethods, TrackRatingMethods}


trait Injectors {
  lazy val appBuilder = new GuiceApplicationBuilder().configure(Configuration.from(Map(
        "slick.dbs.default.driver" -> "slick.driver.PostgresDriver$",
        "slick.dbs.default.db.driver" -> "org.postgresql.Driver",
        "slick.dbs.default.db.url" -> "jdbc:postgresql://123.168.0.10:5432/tests",
        "slick.dbs.default.db.user" -> "simon",
        "slick.dbs.default.db.password" -> "root",
        "slick.dbs.default.db.connectionTimeout" -> "5 seconds",
        "slick.dbs.default.db.connectionPool" -> "disabled")))
  lazy val injector = appBuilder.injector()
  lazy val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  lazy val actorSystem = ActorSystem()
  lazy val duplicateTracksActorInstance = new DuplicateTracksActorInstance(actorSystem)
  lazy val trackMethods = new TrackMethods(dbConfProvider, duplicateTracksActorInstance)
  lazy val genreMethods = new GenreMethods(dbConfProvider)
  lazy val searchSoundCloudTracks = new SearchSoundCloudTracks(trackMethods, genreMethods)
  lazy val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, trackMethods)
  lazy val geographicPointMethods = new SearchGeographicPoint(dbConfProvider)
  lazy val tariffMethods = new TariffMethods(dbConfProvider)
  lazy val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, addressMethods)
  lazy val addressMethods = new AddressMethods(dbConfProvider, geographicPointMethods)
  lazy val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, addressMethods, geographicPointMethods)
  lazy val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods)
  lazy val eventMethods = new EventMethods(dbConfProvider, organizerMethods, artistMethods, tariffMethods, trackMethods,
    genreMethods, placeMethods, geographicPointMethods, addressMethods)
  lazy val userDAOImpl = new UserDAOImpl(dbConfProvider)
  lazy val playlistMethods = new PlaylistMethods(dbConfProvider)
  lazy val trackRatingMethods = new TrackRatingMethods(dbConfProvider, trackMethods)
  lazy val issueMethods = new IssueMethods(dbConfProvider)
  lazy val oAuth2InfoDAO = new OAuth2InfoDAO(dbConfProvider)
  lazy val getUserLikedPagesOnFacebook = new GetUserLikedPagesOnFacebook(dbConfProvider, oAuth2InfoDAO, artistMethods,
    placeMethods, organizerMethods, eventMethods, trackMethods)

  lazy val databaseApi = injector.instanceOf[DBApi]
}
