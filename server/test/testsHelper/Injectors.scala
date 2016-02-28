package testsHelper

import addresses.{AddressMethods, SearchGeographicPoint}
import akka.actor.ActorSystem
import application.UserMethods
import artistsDomain.ArtistMethods
import attendees.AttendeeMethods
import eventsDomain.EventMethods
import genresDomain.GenreMethods
import issues.IssueMethods
import jobs.Scheduler
import organizersDomain.OrganizerMethods
import placesDomain.PlaceMethods
import play.api.Configuration
import play.api.db.DBApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import playlistsDomain.PlaylistMethods
import services.GetUserLikedPagesOnFacebook
import silhouette.{OAuth2InfoDAO, UserDAOImpl}
import tariffsDomain.TariffMethods
import ticketsDomain.TicketMethods
import trackingDomain.TrackingMethods
import tracksDomain._


trait Injectors {
  lazy val appBuilder = new GuiceApplicationBuilder().configure(Configuration.from(Map(
    "slick.dbs.default.driver" -> "slick.driver.PostgresDriver$",
    "slick.dbs.default.db.driver" -> "org.postgresql.Driver",
    "slick.dbs.default.db.url" -> "jdbc:postgresql://dbHostTest:5432/tests",
    "slick.dbs.default.db.user" -> "simon",
    "slick.dbs.default.db.password" -> "root",
    "slick.dbs.default.db.connectionTimeout" -> "5 seconds",
    "slick.dbs.default.db.connectionPool" -> "disabled")))
  lazy val injector = appBuilder.injector()
  lazy val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  lazy val databaseApi = injector.instanceOf[DBApi]
  lazy val actorSystem = ActorSystem()
  lazy val duplicateTracksActorInstance = new DuplicateTracksActorInstance(actorSystem)
  lazy val trackMethods = new TrackMethods(dbConfProvider, duplicateTracksActorInstance)
  lazy val genreMethods = new GenreMethods(dbConfProvider)
  lazy val searchSoundCloudTracks = new SearchSoundCloudTracks(trackMethods, genreMethods)
  lazy val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, trackMethods)
  lazy val geographicPointMethods = new SearchGeographicPoint(dbConfProvider)
  lazy val attendeesMethods = new AttendeeMethods(dbConfProvider)
  lazy val tariffMethods = new TariffMethods(dbConfProvider)
  lazy val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, addressMethods)
  lazy val addressMethods = new AddressMethods(dbConfProvider, geographicPointMethods)
  lazy val ticketMethods = new TicketMethods(dbConfProvider)
  lazy val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, addressMethods, geographicPointMethods)
  lazy val artistMethods = new ArtistMethods(dbConfigProvider = dbConfProvider, genreMethods = genreMethods,
    searchSoundCloudTracks = searchSoundCloudTracks, searchYoutubeTracks = searchYoutubeTrack, trackMethods = trackMethods)
  lazy val eventMethods = new EventMethods(dbConfigProvider = dbConfProvider, organizerMethods = organizerMethods,
    artistMethods = artistMethods, tariffMethods = tariffMethods, trackMethods = trackMethods,
    genreMethods = genreMethods, placeMethods = placeMethods, geographicPointMethods = geographicPointMethods,
    addressMethods = addressMethods)
  lazy val userDAOImpl = new UserDAOImpl(dbConfProvider)
  lazy val playlistMethods = new PlaylistMethods(dbConfProvider)
  lazy val trackRatingMethods = new TrackRatingMethods(dbConfProvider, trackMethods)
  lazy val issueMethods = new IssueMethods(dbConfProvider)
  lazy val userMethods = new UserMethods(dbConfProvider)
  lazy val trackingMethods = new TrackingMethods(dbConfProvider)
  lazy val oAuth2InfoDAO = new OAuth2InfoDAO(dbConfProvider)
  lazy val getUserLikedPagesOnFacebook = new GetUserLikedPagesOnFacebook(dbConfigProvider = dbConfProvider,
    oAuth2InfoDAO = oAuth2InfoDAO, artistMethods = artistMethods, placeMethods = placeMethods,
    organizerMethods = organizerMethods, eventMethods = eventMethods, trackMethods = trackMethods)
  lazy val scheduler = new Scheduler(eventMethods = eventMethods, organizerMethods = organizerMethods,
    artistMethods = artistMethods, trackMethods = trackMethods, placeMethods = placeMethods,
    addressMethods = addressMethods, searchGeographicPoint = geographicPointMethods)
}
