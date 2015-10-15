package models

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject


import com.vividsolutions.jts.geom.Point
import controllers.{DAOException, ThereIsNoArtistForThisFacebookIdException}
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.{MyPostgresDriver, SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import services.MyPostgresDriver.api._

case class Artist (id: Option[Long],
                   facebookId: Option[String],
                   name: String,
                   imagePath: Option[String] = None,
                   description: Option[String] = None,
                   facebookUrl: String,
                   websites: Set[String] = Set.empty)
//                   genres: Seq[Genre] = Seq.empty,
//                   tracks: Seq[Track] = Seq.empty,
//                   likes: Option[Int] = None,
//                   country: Option[String] = None)

class ArtistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                       val organizerMethods: OrganizerMethods,
                       val placeMethods: PlaceMethods,
                       val eventMethods: EventMethods,
                       val genreMethods: GenreMethods,
                       val searchSoundCloudTracks: SearchSoundCloudTracks,
                       val searchYoutubeTracks: SearchYoutubeTracks,
                       val trackMethods: TrackMethods,
                       val utilities: Utilities) extends HasDatabaseConfigProvider[MyPostgresDriver] {

  import eventMethods.EventArtistRelation

  class Artists(tag: Tag) extends Table[Artist](tag, "artists") {
    def id = column[Long]("organizerId", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def name = column[String]("name")
    def imagePath = column[Option[String]]("imagepath")
    def description = column[Option[String]]("description")
    def facebookUrl = column[String]("facebookurl")
    def websites = column[Option[String]]("websites")

    def * = (id.?, facebookId, name, imagePath, description, facebookUrl, websites).shaped <> (
      { case (id, facebookId, name, imagePath, description, facebookUrl, websites) =>
      Artist(id, facebookId, name, imagePath, description, facebookUrl, utilities.optionStringToSet(websites))
    }, { artist: Artist =>
      Some((artist.id, artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
        Option(artist.websites.mkString(","))))
    })
//    def * = (id.?, facebookId, name, imagePath, description, facebookUrl, websites, likes, country) <>
//      ((Artist.apply _).tupled, Artist.unapply)
  }

  case class UserArtistRelation(userId: String, artistId: Long)

  class ArtistsFollowed(tag: Tag) extends Table[UserArtistRelation](tag, "artistsfollowed") {
    def userId = column[String]("userid")
    def artistId = column[Long]("artistid")

    def * = (userId, artistId) <> ((UserArtistRelation.apply _).tupled, UserArtistRelation.unapply)
  }

  lazy val artistsFollowed = TableQuery[ArtistsFollowed]

  case class ArtistGenreRelation(artistId: Long, genreId: Int)

  class ArtistsGenres(tag: Tag) extends Table[ArtistGenreRelation](tag, "artistsGenres") {
    def artistId = column[Long]("artistid")
    def genreId = column[Int]("genreid")

    def * = (artistId, genreId) <> ((ArtistGenreRelation.apply _).tupled, ArtistGenreRelation.unapply)

    def aFK = foreignKey("artistid", artistId, artists)(_.id, onDelete=ForeignKeyAction.Cascade)
    def bFK = foreignKey("genreid", genreId, genres)(_.id, onDelete=ForeignKeyAction.Cascade)
  }

  lazy val artistsGenres = TableQuery[ArtistsGenres]

  lazy val artists = TableQuery[Artists]
  val events = eventMethods.events
  val eventsArtists = eventMethods.eventsArtists
  val genres = genreMethods.genres
  val facebookToken = utilities.facebookToken
  val soundCloudClientId = utilities.soundCloudClientId

//  def formApply(facebookId: Option[String], name: String, imagePath: Option[String], description: Option[String],
//                facebookUrl: String, websites: Seq[String], genres: Seq[Genre], tracks: Seq[Track], likes: Option[Int],
//                country: Option[String]): Artist =
//    Artist(None, facebookId, name, imagePath, description, facebookUrl, websites.toSet, genres, tracks, likes, country)
//  def formUnapply(artist: Artist) =
//    Option((artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
//      artist.websites.toSeq, artist.genres, artist.tracks, artist.likes, artist.country))

  case class PatternAndArtist (searchPattern: String, artist: Artist)
  def formWithPatternApply(searchPattern: String, artist: Artist) =
    new PatternAndArtist(searchPattern, artist)
  def formWithPatternUnapply(searchPatternAndArtist: PatternAndArtist) =
    Option((searchPatternAndArtist.searchPattern, searchPatternAndArtist.artist))
  
//  def getArtistProperties(artist: Artist): Artist = artist.copy(
//      tracks = Track.findAllByArtist(artist.facebookUrl, 0, 0),
//      genres = Genre.findAllByArtist(artist.id.getOrElse(-1L).toInt)
//  )

  def findAll: Future[Seq[Artist]] =  db.run(artists.result)//        .map(getArtistProperties)

  def findSinceOffset(numberToReturn: Int, offset: Int): Future[Seq[Artist]] = {
    val query = artists.drop(offset).take(numberToReturn)
    db.run(query.result)
//        .map(getArtistProperties)
  }
  
  def findAllByEvent(event: Event): Future[Seq[Artist]] = {
    val query = for {
      e <- events if e.id === event.id
      eventArtist <- eventsArtists
      artist <- artists if artist.id === eventArtist.artistId 
    } yield artist

    db.run(query.result)
    //.map(getArtistProperties)
  }
  
  def findAllByGenre(genreName: String, offset: Int, numberToReturn: Int): Future[Seq[Artist]] = {
    val query = for {
      genre <- genres if genre.name === genreName
      artistGenre <- artistsGenres
      artist <- artists if artist.id === artistGenre.artistId 
    } yield artist

    //getArtistProperties
    db.run(query.drop(numberToReturn).take(offset).result)
  }
  
  def find(id: Long): Future[Option[Artist]] = {
    val query = artists.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def findByFacebookUrl(facebookUrl: String): Future[Option[Artist]] = {
    val query = artists.filter(_.facebookUrl === facebookUrl)
    db.run(query.result.headOption)
  }

  def findAllContaining(pattern: String): Future[Seq[Artist]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      artist <- artists if artist.name.toLowerCase like s"%$lowercasePattern%"
    } yield artist

    db.run(query.take(10).result)
  }

  def findIdByName(name: String): Future[Option[Long]] =
    db.run(artists.filter(_.name === name).map(_.id).result.headOption)

  def findIdByFacebookId(facebookId: String): Future[Option[Long]] =
    db.run(artists.filter(_.facebookId === facebookId).map(_.id).result.headOption)

  def findIdByFacebookUrl(facebookUrl: String): Future[Option[Long]] =
    db.run(artists.filter(_.facebookUrl === facebookUrl).map(_.id).result.headOption)
  
  def save(artist: Artist): Future[Artist] = {
    val artistWithFormattedDescription = artist.copy(description = utilities.formatDescription(artist.description))
    val insertQuery = artists returning artists.map(_.id) into ((artist, id) => artist.copy(id = Option(id)))
    val action = insertQuery += artistWithFormattedDescription

    db.run(action)
    /*
       case Some(artistId: Long) =>
              artist.genres.foreach { Genre.saveWithArtistRelation(_, artistId) }
              artist.tracks.foreach { Track.save }
     */
  }

  def update(artist: Artist): Future[Int] = db.run(artists.filter(_.id === artist.id).update(artist))

  def saveArtistsAndTheirTracks(artists: Seq[Artist]): Unit = Future {
    artists.map { artist =>
      save(artist) map { artistSaved =>
        getArtistTracks(PatternAndArtist(artistSaved.name, artistSaved)) |>> Iteratee.foreach( a => a.map { trackMethods.save })
      }
    }
  }

  def getArtistTracks(patternAndArtist: PatternAndArtist): Enumerator[Set[Track]] = {
    val soundCloudTracksEnumerator = Enumerator.flatten(
      searchSoundCloudTracks.getSoundCloudTracksForArtist(patternAndArtist.artist).map { soundCloudTracks =>
        addSoundCloudWebsiteIfMissing(soundCloudTracks.headOption, patternAndArtist.artist)
        Enumerator(soundCloudTracks.toSet)
      })

    val youtubeTracksEnumerator =
      searchYoutubeTracks.getYoutubeTracksForArtist(patternAndArtist.artist, patternAndArtist.searchPattern)

    Enumerator.interleave(soundCloudTracksEnumerator, youtubeTracksEnumerator).andThen(Enumerator.eof)
  }

  def addWebsite(artistId: Long, normalizedUrl: String): Future[Int] = {
//    val query = artists.filter(_.id == artistId).map(_.update()
    val query = sqlu"""UPDATE artists
          |  SET websites = case
          |    WHEN websites IS NULL THEN $normalizedUrl
          |    ELSE websites || ',' || $normalizedUrl
          |  END
          |WHERE artistId = $artistId"""
    db.run(query)
  }

  def addSoundCloudWebsiteIfMissing(soundCloudTrack: Option[Track], artist: Artist): Unit = soundCloudTrack match {
    case None =>
    case Some(soundCloudTrack: Track) =>
      soundCloudTrack.redirectUrl match {
        case None =>
        case Some(redirectUrl) =>
          val refactoredRedirectUrl = searchSoundCloudTracks.removeUselessInSoundCloudWebsite(utilities.normalizeUrl(redirectUrl))
          if (!artist.websites.contains(refactoredRedirectUrl) && artist.id.nonEmpty)
            addWebsite(artist.id.get, refactoredRedirectUrl)
      }
  }

  def saveWithEventRelation(artist: Artist, eventId: Long): Future[Int] = save(artist) flatMap { artist =>
    saveEventRelation(EventArtistRelation(eventId, artist.id.getOrElse(0)))
  }

  def saveEventRelation(eventArtistRelation: EventArtistRelation): Future[Int] =
    db.run(eventsArtists += eventArtistRelation)

  def deleteEventRelation(eventArtistRelation: EventArtistRelation): Future[Int] = db.run(
    eventsArtists
      .filter(artistFollowed =>
        artistFollowed.eventId === eventArtistRelation.artistId && artistFollowed.eventId === eventArtistRelation.eventId)
      .delete)

  def delete(id: Long): Future[Int] = db.run(artists.filter(_.id === id).delete)
  
  def followByArtistId(userArtistRelation: UserArtistRelation): Future[Int] = db.run(artistsFollowed += userArtistRelation)

  def unfollowByArtistId(userArtistRelation: UserArtistRelation): Future[Int] = db.run(
    artistsFollowed
      .filter(artistFollowed =>
      artistFollowed.userId === userArtistRelation.userId && artistFollowed.artistId === userArtistRelation.artistId)
      .delete)

  def followByFacebookId(userId : String, facebookId: String): Future[Int] = findIdByFacebookId(facebookId) flatMap {
    case None =>
      Logger.error("Artist.followByFacebookId: ", ThereIsNoArtistForThisFacebookIdException("Artist.followByFacebookId"))
      Future { 0 }
    case Some(artistId) =>
      followByArtistId(UserArtistRelation(userId, artistId))
  }
  
  def getFollowedArtists(userId: String): Future[Seq[Artist] ]= {
    val query = for {
      artistFollowed <- artistsFollowed if artistFollowed.userId === userId
      artist <- artists if artist.id === artistFollowed.artistId
    } yield artist

    db.run(query.result)
  }
  
  def isFollowed(userId: String, artistId: Long): Future[Boolean] = {
    val query = sql"""SELECT exists(SELECT 1 FROM artistsFollowed WHERE userId = $userId AND artistId = $artistId)"""
      .as[Boolean]
    db.run(query.head)
  }

  def normalizeArtistName(artistName: String): String = utilities.normalizeString(artistName)
    .toLowerCase
    .replaceAll("officiel", "")
    .replaceAll("fanpage", "")
    .replaceAll("official", "")
    .replaceAll("fb", "")
    .replaceAll("facebook", "")
    .replaceAll("page", "")
    .trim
    .replaceAll("""\s+""", " ")

  def splitArtistNamesInTitle(title: String): List[String] =
   "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "")

  val facebookArtistFields = "name,cover{source,offset_x,offset_y},id,category,link,website,description,genre,location,likes"

  def getEventuallyFacebookArtists(pattern: String): Future[Seq[Artist]] = {
    WS.url("https://graph.facebook.com/v2.2/search")
     .withQueryString(
       "q" -> pattern,
       "type" -> "page",
       "limit" -> "400",
       "fields" -> facebookArtistFields,
       "access_token" -> facebookToken)
     .get()
     .map { readFacebookArtists }
  }
 
  def getEventuallyArtistsInEventTitle(artistsNameInTitle: Seq[String], webSites: Set[String]): Future[Seq[Artist]] = {
    Future.sequence(
     artistsNameInTitle.map {
       getEventuallyFacebookArtists(_).map { artists => artists }
     }
    ).map { _.flatten collect { case artist: Artist if (artist.websites intersect webSites).nonEmpty => artist } }
  }

  def getFacebookArtistsByWebsites(websites: Set[String]): Future[Set[Option[Artist]]] = {
    Future.sequence(
     websites.map {
       case website if website contains "facebook" =>
         getFacebookArtistByFacebookUrl(website).map { maybeFacebookArtist => maybeFacebookArtist }
       case website if website contains "soundcloud" =>
         getMaybeFacebookUrlBySoundCloudUrl(website) flatMap {
           case None =>
             Future { None }
           case Some(facebookUrl) =>
             getFacebookArtistByFacebookUrl(facebookUrl).map { maybeFacebookArtist => maybeFacebookArtist }
         }
       case _ =>
         Future { None }
     }
    )
  }

  def getMaybeFacebookUrlBySoundCloudUrl(soundCloudUrl: String): Future[Option[String]] = {
    val soundCloudName = soundCloudUrl.substring(soundCloudUrl.indexOf("/") + 1)
    WS.url("http://api.soundcloud.com/users/" + soundCloudName + "/web-profiles")
     .withQueryString("client_id" -> soundCloudClientId)
     .get()
     .map { readMaybeFacebookUrl }
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesWSResponse: WSResponse): Option[String] = {
    val facebookUrlReads = (
     (__ \ "url").read[String] and
       (__ \ "service").read[String]
     )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
     urlService.collect { case (url: String, "facebook") => utilities.normalizeUrl(url) }
    }

    soundCloudWebProfilesWSResponse.json.asOpt[Seq[String]](collectOnlyFacebookUrls) match {
     case Some(facebookUrls: Seq[String]) if facebookUrls.nonEmpty => Option(facebookUrls.head)
     case _ => None

    }
  }

  def getFacebookArtistByFacebookUrl(url: String): Future[Option[Artist]] = {
    WS.url("https://graph.facebook.com/v2.2/" + normalizeFacebookUrl(url))
     .withQueryString(
       "fields" -> facebookArtistFields,
       "access_token" -> facebookToken)
     .get()
     .map { readFacebookArtist }
  }

  def normalizeFacebookUrl(facebookUrl: String): String = {
    val firstNormalization = facebookUrl.drop(facebookUrl.lastIndexOf("/") + 1) match {
     case urlWithProfile: String if urlWithProfile contains "profile.php?id=" =>
       urlWithProfile.substring(urlWithProfile.lastIndexOf("=") + 1)
     case alreadyNormalizedUrl: String =>
       alreadyNormalizedUrl
    }
    firstNormalization match {
     case urlWithArguments if urlWithArguments contains "?" =>
       urlWithArguments.slice(0, urlWithArguments.lastIndexOf("?"))
     case urlWithoutArguments =>
       urlWithoutArguments
    }
  }

  val readArtist = (
   (__ \ "name").read[String] and
     (__ \ "category").read[String] and
     (__ \ "id").read[String] and
     (__ \ "cover").readNullable[String](
       (__ \ "source").read[String]
     ) and
     (__ \ "cover").readNullable[Int](
       (__ \ "offset_x").read[Int]
     ) and
     (__ \ "cover").readNullable[Int](
       (__ \ "offset_y").read[Int]
     ) and
     (__ \ "website").readNullable[String] and
     (__ \ "link").read[String] and
     (__ \ "description").readNullable[String] and
     (__ \ "genre").readNullable[String] and
     (__ \ "likes").readNullable[Int] and
     (__ \ "location").readNullable[Option[String]](
       (__ \ "country").readNullable[String]
     )
   ).apply((name: String, category: String, id: String, maybeCover: Option[String], maybeOffsetX: Option[Int],
            maybeOffsetY: Option[Int], websites: Option[String], link: String, maybeDescription: Option[String],
            maybeGenre: Option[String], maybeLikes: Option[Int], maybeCountry: Option[Option[String]]) =>
   (name, id, category, maybeCover, maybeOffsetX, maybeOffsetY, websites, link, maybeDescription, maybeGenre,
     maybeLikes, maybeCountry))

  def readFacebookArtists(facebookWSResponse: WSResponse): Seq[Artist] = {
   val collectOnlyArtistsWithCover: Reads[Seq[Artist]] = Reads.seq(readArtist).map { artists =>
     artists.collect {
       case (name, facebookId, category, Some(cover: String), maybeOffsetX, maybeOffsetY, websites, link,
       maybeDescription, maybeGenre, maybeLikes, maybeCountry)
         if category.equalsIgnoreCase("Musician/band") | category.equalsIgnoreCase("Artist") =>
         makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), websites, link,
           maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten)
     }
   }
   (facebookWSResponse.json \ "data")
     .asOpt[Seq[Artist]](collectOnlyArtistsWithCover)
     .getOrElse(Seq.empty)
  }

  def readFacebookArtist(facebookWSResponse: WSResponse): Option[Artist] = {
   facebookWSResponse.json
     .asOpt[(String, String, String, Option[String], Option[Int], Option[Int], Option[String], String,
     Option[String], Option[String], Option[Int], Option[Option[String]])](readArtist)
   match {
     case Some((name, facebookId, "Musician/band", Some(cover: String), maybeOffsetX, maybeOffsetY, maybeWebsites,
     link, maybeDescription, maybeGenre, maybeLikes, maybeCountry)) =>
       Option(makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), maybeWebsites,
         link, maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten))
     case Some((name, facebookId, "Artist", Some(cover: String), maybeOffsetX, maybeOffsetY, maybeWebsites,
     link, maybeDescription, maybeGenre, maybeLikes, maybeCountry)) =>
       Option(makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), maybeWebsites,
         link, maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten))
     case _ => None
   }
  }

  def makeArtist(name: String, facebookId: String, cover: String, maybeWebsites: Option[String], link: String,
                maybeDescription: Option[String], maybeGenre: Option[String], maybeLikes: Option[Int],
                maybeCountry: Option[String]): Artist = {
   val facebookUrl = utilities.normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
   val websitesSet = utilities.getNormalizedWebsitesInText(maybeWebsites)
     .filterNot(_.contains("facebook.com"))
     .filterNot(_ == "")
   val description = utilities.formatDescription(maybeDescription)
   val genres = genreMethods.genresStringToGenresSet(maybeGenre)
   Artist(None, Option(facebookId), name, Option(cover), description, facebookUrl, websitesSet/*, genres.toSeq,
     Seq.empty, maybeLikes, maybeCountry*/)
  }

  def aggregateImageAndOffset(imgUrl: String, offsetX: Option[Int], offsetY: Option[Int]): String =
    imgUrl + """\""" + offsetX.getOrElse(0).toString + """\""" + offsetY.getOrElse(0).toString
}
