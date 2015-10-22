package models

import java.sql.{JDBCType, Connection}
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
import services._
import slick.jdbc.{PositionedParameters, SetParameter}

import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import services.MyPostgresDriver.api._
import scala.concurrent.duration._


case class Artist(id: Option[Long],
                  facebookId: Option[String],
                  name: String,
                  imagePath: Option[String] = None,
                  description: Option[String] = None,
                  facebookUrl: String,
                  websites: Set[String] = Set.empty,
//                   genres: Seq[Genre] = Seq.empty,
//                   tracks: Seq[Track] = Seq.empty,
                  likes: Option[Int] = None,
                  country: Option[String] = None)

case class ArtistWithGenres(artist: Artist, genres: Seq[Genre])

case class PatternAndArtist(searchPattern: String, artist: Artist)

class ArtistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                       val genreMethods: GenreMethods,
                       val searchSoundCloudTracks: SearchSoundCloudTracks,
                       val searchYoutubeTracks: SearchYoutubeTracks,
                       val trackMethods: TrackMethods,
                       val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with SoundCloudHelper with MyDBTableDefinitions {

  val facebookToken = utilities.facebookToken
  val soundCloudClientId = utilities.soundCloudClientId

  def formApply(facebookId: Option[String], name: String, imagePath: Option[String], description: Option[String],
                facebookUrl: String, websites: Seq[String]/*, genres: Seq[Genre], tracks: Seq[Track]*/, likes: Option[Int],
                country: Option[String]): Artist =
    Artist(None, facebookId, name, imagePath, description, facebookUrl, websites.toSet/*, genres, tracks*/, likes, country)
  def formUnapply(artist: Artist) =
    Option((artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
      artist.websites.toSeq/*, artist.genres, artist.tracks*/, artist.likes, artist.country))

  def formWithPatternApply(searchPattern: String, artist: Artist) =
    new PatternAndArtist(searchPattern, artist)
  def formWithPatternUnapply(searchPatternAndArtist: PatternAndArtist) =
    Option((searchPatternAndArtist.searchPattern, searchPatternAndArtist.artist))
  
//  def getArtistProperties(artist: Artist): Artist = artist.copy(
//      tracks = Track.findAllByArtist(artist.facebookUrl, 0, 0),
//      genres = Genre.findAllByArtist(artist.id.getOrElse(-1L).toInt)
//  )

  def findAll: Future[Seq[Artist]] = db.run(artists.result)//        .map(getArtistProperties)
//
//  Vector(
//    (
//      Artist(Some(261),Some(a),a,Some(a),Some(a),a,Set(a),None,None),
//      Some((Genre(Some(71),a,a),ArtistGenreRelation(261,71,1)))
//      ), (
//      Artist(Some(261),Some(a),a,Some(a),Some(a),a,Set(a),None,None),
//      Some((Genre(Some(72),b,b),ArtistGenreRelation(261,72,1)))
//      ), (
//      Artist(Some(284),Some(facebookIdTestArtistModel1),artistTest1,Some(imagePath), Some(<div class='column large-12'>description</div>),facebookUrl1,Set(website),None,None),
//      None)
//  )
//
//
//
//  Vector(
//    (Some((Genre(Some(71),a,a),ArtistGenreRelation(261,71,1))), Artist(Some(261),Some(a),a,Some(a),Some(a),a,Set(a),None,None)),
//    (Some((Genre(Some(72),b,b),ArtistGenreRelation(261,72,1))),Artist(Some(261),Some(a),a,Some(a),Some(a),a,Set(a),None,None)),
//    (None,Artist(Some(291),Some(facebookIdTestArtistModel1),artistTest1,Some(imagePath),Some(<div class='column large-12'>description</div>),facebookUrl1,Set(website),None,None))
//  )

//  Map(
//    Artist(Some(298),Some(facebookIdTestArtistModel1),artistTest1,Some(imagePath),Some(<div class='column large-12'>description</div>),
//      facebookUrl1,Set(website),None,None) ->
//      Vector((None,
//        Artist(Some(298),Some(facebookIdTestArtistModel1),artistTest1,Some(imagePath),Some(<div class='column large-12'>description</div>),
//        facebookUrl1,Set(website),None,None))),
//    Artist(Some(261),Some(a),a,Some(a),Some(a),a,Set(a),None,None) ->
//      Vector((Some((Genre(Some(71),a,a),ArtistGenreRelation(261,71,1))),
//        Artist(Some(261),Some(a),a,Some(a),Some(a),a,Set(a),None,None)),
//        (Some((Genre(Some(72),b,b),ArtistGenreRelation(261,72,1))),Artist(Some(261),Some(a),a,Some(a),Some(a),a,Set(a),None,None))))



//  def findSinceOffset(numberToReturn: Int, offset: Int): Future[Seq[ArtistWithGenres]] = {
//    val query = genres join
//      artistsGenres on (_.id === _.genreId) joinRight
//      artists on (_._2.artistId === _.id)
//
//    val action = query.drop(offset).take(numberToReturn).result
//    db.run(action) map { artistWithRelations =>
//       artistWithRelations.groupBy(_._2).map(c => (c._1, c._2.flatMap(d => d._1.map(_._1)))).toList map (e => ArtistWithGenres(e._1, e._2))
//    }
////        .map(getArtistProperties)
//  }

  def findSinceOffset(numberToReturn: Int, offset: Int): Future[Seq[ArtistWithGenres]] = {
    val query = /*tracks join*/
      genres join
      artistsGenres on (_.id === _.genreId) joinRight
      artists on (_._2.artistId === _.id)

    val action = query.drop(offset).take(numberToReturn).result
    db.run(action) map { artistWithRelations =>
      artistWithRelations.groupBy(_._2).map(c => (c._1, c._2.flatMap(d => d._1.map(_._1)))).toList map (e => ArtistWithGenres(e._1, e._2))
    }
    //        .map(getArtistProperties)
  }

  def findAllByEvent(event: Event): Future[Seq[Artist]] = {
    val query2 = for {
      e <- events if e.id === event.id
      eventArtist <- eventsArtists
      artist <- artists if artist.id === eventArtist.artistId
      artistGenre <- artistsGenres if artistGenre.artistId === artist.id
      genre <- genres if genre.id === artistGenre.genreId
//      artistGenre <- artists joinLeft artistsGenres on (artistGenre.artistId === artist.id)
    } yield (artist, genre)

    val a = Await.result(db.run(query2.result), 3 seconds)

    println(a)

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
    db.run((for {
      artistFound <- artists.filter(_.facebookUrl === artist.facebookUrl).result.headOption
      result <- artistFound.map(DBIO.successful).getOrElse(artists returning artists.map(_.id) += artistWithFormattedDescription)
    } yield result match {
      case a: Artist => a
      case id: Long => artistWithFormattedDescription.copy(id = Option(id))
    }).transactionally)
    /*
       case Some(artistId: Long) =>
              artist.genres.foreach { Genre.saveWithArtistRelation(_, artistId) }
              artist.tracks.foreach { Track.save }
     */
    /*
    Done BY Loann:
     .as(scalar[Long].singleOpt) match {
          case Some(artistId: Long) =>
            val genresWithOverGenres = (artist.genres ++ Genre.findOverGenres(artist.genres)).distinct
            genresWithOverGenres.foreach { Genre.saveWithArtistRelation(_, artistId.toInt) }
            artist.tracks.foreach { Track.save }
            Option(artistId)
          case None =>
            None
      }


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
        soundCloudTracks.headOption match {
          case Some(track) =>
            addSoundCloudUrlIfMissing(track, patternAndArtist.artist)
            addWebsitesFoundOnSoundCloud(track, patternAndArtist.artist)
          case None =>
        }
        Enumerator(soundCloudTracks.toSet).andThen(Enumerator.eof)
      })

    val youtubeTracksEnumerator =
      searchYoutubeTracks.getYoutubeTracksForArtist(patternAndArtist.artist, patternAndArtist.searchPattern).andThen(Enumerator.eof)

    val youtubeTracksFromChannel = Enumerator.flatten(searchYoutubeTracks.getYoutubeTracksByChannel(patternAndArtist.artist) map { track =>
     Enumerator(track).andThen(Enumerator.eof)
    } )

    val youtubeTracksFromYoutubeUser = Enumerator.flatten(
      searchYoutubeTracks.getYoutubeTracksByYoutubeUser(patternAndArtist.artist) map { track =>
        Enumerator(track).andThen(Enumerator.eof)
      })

    Enumerator.interleave(soundCloudTracksEnumerator, youtubeTracksEnumerator, youtubeTracksFromChannel,
      youtubeTracksFromYoutubeUser).andThen(Enumerator.eof)
  }

  def addWebsite(artistId: Long, normalizedUrl: String): Future[Int] = {
    val query = artists.filter(_.id === artistId)

    val action = for {
      artist <- query.result.headOption
      updatedArtist <- query.map(_.websites).update(Option(artist.get.websites.mkString(",") + "," + normalizedUrl))
    } yield updatedArtist

    db.run(action)
  }

  def addSoundCloudUrlIfMissing(soundCloudTrack: Track, artist: Artist): Future[Int] = soundCloudTrack.redirectUrl match {
    case None =>
      Future(0)
    case Some(redirectUrl) =>
      val refactoredRedirectUrl = removeUselessInSoundCloudWebsite(utilities.normalizeUrl(redirectUrl))
      if (!artist.websites.contains(refactoredRedirectUrl) && artist.id.nonEmpty)
        addWebsite(artist.id.get, refactoredRedirectUrl)
      else
        Future(0)
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

  def followByFacebookId(userId : UUID, facebookId: String): Future[Int] = findIdByFacebookId(facebookId) flatMap {
    case None =>
      Logger.error("Artist.followByFacebookId: ", ThereIsNoArtistForThisFacebookIdException("Artist.followByFacebookId"))
      Future { 0 }
    case Some(artistId) =>
      followByArtistId(UserArtistRelation(userId, artistId))
  }
  
  def getFollowedArtists(userId: UUID): Future[Seq[Artist] ]= {
    val query = for {
      artistFollowed <- artistsFollowed if artistFollowed.userId === userId
      artist <- artists if artist.id === artistFollowed.artistId
    } yield artist

    db.run(query.result)
  }

  def isFollowed(userArtistRelation: UserArtistRelation): Future[Boolean] = {
    val query =
      sql"""SELECT exists(SELECT 1 FROM artistsFollowed WHERE userId = ${userArtistRelation.userId}
           AND artistId = ${userArtistRelation.artistId})"""
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

  def getEventuallyArtistsInEventTitle(artistsNameInTitle: Seq[String], webSites: Set[String]): Future[Seq[Artist]] =
    Future.sequence(
      artistsNameInTitle.map { name => getArtistsForAnEvent(name, webSites) }
    ).map { _.flatten }

  def getArtistsForAnEvent(artistName: String, eventWebSites: Set[String]): Future[Seq[Artist]] = {
    getEventuallyFacebookArtists(artistName).flatMap {
      case noArtist if noArtist.isEmpty && artistName.split("\\W+").size >= 2 =>
        val nestedEventuallyArtists = artistName.split("\\W+").toSeq.map { getArtistsForAnEvent(_, eventWebSites) }
        Future.sequence(nestedEventuallyArtists) map { _.flatten }
      case artists =>
        Future  { filterFacebookArtistsForEvent(artists, artistName, eventWebSites)}
    }
  }

  def filterFacebookArtistsForEvent(artists: Seq[Artist], artistName: String, eventWebsites: Set[String]): Seq[Artist] =
    artists match {
    case onlyOneArtist: Seq[Artist] if onlyOneArtist.size == 1 && onlyOneArtist.head.name.toLowerCase == artistName =>
      onlyOneArtist
    case otherCase: Seq[Artist] =>
      val artists = otherCase.flatMap { artist: Artist =>
        if ((artist.websites intersect eventWebsites).nonEmpty) Option(artist)
        else None
      }
      artists
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
      .withQueryString("client_id" -> utilities.soundCloudClientId)
      .get()
      .map { readMaybeFacebookUrl }
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesResponse: WSResponse): Option[String] = {
    val facebookUrlReads = (
      (__ \ "url").read[String] and
        (__ \ "service").read[String]
      )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
      urlService.collect { case (url: String, "facebook") => utilities.normalizeUrl(url) }
    }

    soundCloudWebProfilesResponse.json.asOpt[Seq[String]](collectOnlyFacebookUrls) match {
      case Some(facebookUrls: Seq[String]) if facebookUrls.nonEmpty => Option(facebookUrls.head)
      case _ => None
    }
  }

  def normalizeFacebookUrl(facebookUrl: String): String = {
    val firstNormalization = facebookUrl.toLowerCase match {
      case urlWithProfile: String if urlWithProfile contains "profile.php?id=" =>
        Option(urlWithProfile.substring(urlWithProfile.lastIndexOf("=") + 1))
      case alreadyNormalizedUrl: String =>
        if (alreadyNormalizedUrl.indexOf("facebook.com/") > -1) {
          val normalizedUrl = alreadyNormalizedUrl.substring(alreadyNormalizedUrl.indexOf("facebook.com/") + 13)
          if (normalizedUrl.indexOf("pages/") > -1) {
            val idRegex = new Regex("/[0-9]+")
            idRegex.findAllIn(normalizedUrl).toSeq.headOption match {
              case Some(id) => Option(id.replace("/", ""))
              case None => None
            }
          } else if (normalizedUrl.indexOf("/") > -1) {
            Option(normalizedUrl.take(normalizedUrl.indexOf("/")))
          } else {
            Option(normalizedUrl)
          }
        } else {
          Option(alreadyNormalizedUrl)
        }
    }
    firstNormalization match {
      case Some(urlWithArguments) if urlWithArguments contains "?" =>
        urlWithArguments.slice(0, urlWithArguments.lastIndexOf("?"))
      case Some(urlWithoutArguments) =>
        urlWithoutArguments
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
         if category.equalsIgnoreCase("Musician/Band") | category.equalsIgnoreCase("Artist") =>
         makeArtist(name, facebookId, aggregateImageAndOffset(cover, maybeOffsetX, maybeOffsetY), websites, link,
           maybeDescription, maybeGenre, maybeLikes, maybeCountry.flatten)
     }
   }
   (facebookWSResponse.json \ "data")
     .asOpt[Seq[Artist]](collectOnlyArtistsWithCover)
     .getOrElse(Seq.empty)
  }

  def splitArtistNamesInTitle(title: String): List[String] =
    "@.*".r.replaceFirstIn(title, "").split("[^\\S].?\\W").toList.filter(_ != "").map {
      _.toLowerCase.replace("live", "").replace("djset", "").replace("dj set", "").replace("set", "").trim()
    }

  def readFacebookArtist(facebookWSResponse: WSResponse): Option[Artist] = {
    facebookWSResponse.json
     .asOpt[(String, String, String, Option[String], Option[Int], Option[Int], Option[String], String,
     Option[String], Option[String], Option[Int], Option[Option[String]])](readArtist)
    match {
     case Some((name, facebookId, "Musician/Band", Some(cover: String), maybeOffsetX, maybeOffsetY, maybeWebsites,
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
    val genres = maybeGenre match {
     case Some(genre) => genreMethods.genresStringToGenresSet(genre)
     case None => Set.empty
    }
    Artist(None, Option(facebookId), name, Option(cover), description, facebookUrl, websitesSet/*, genres.toSeq,
     Seq.empty, maybeLikes, maybeCountry*/)
  }

  def aggregateImageAndOffset(imgUrl: String, offsetX: Option[Int], offsetY: Option[Int]): String =
    imgUrl + """\""" + offsetX.getOrElse(0).toString + """\""" + offsetY.getOrElse(0).toString

  def addWebsitesFoundOnSoundCloud(track: Track, artist: Artist): Future[Seq[String]] = track.redirectUrl match {
    case None =>
      Future(Seq.empty)
    case Some(redirectUrl) =>
      val normalizedUrl = removeUselessInSoundCloudWebsite(utilities.normalizeUrl(redirectUrl)).substring("soundcloud.com/".length)
      WS.url("http://api.soundcloud.com/users/" + normalizedUrl + "/web-profiles")
        .withQueryString("client_id" -> utilities.soundCloudClientId)
        .get()
        .map { soundCloudResponse =>
        readSoundCloudWebsites(soundCloudResponse) foreach { website =>
          val normalizedWebsite = utilities.normalizeUrl(website)
          if (!artist.websites.contains(normalizedWebsite) && normalizedWebsite.indexOf("facebook") == -1 && artist.id.nonEmpty)
            addWebsite(artist.id.get, normalizedWebsite) map {
              case res if res != 1 =>
                Logger.error("Artist.addWebsitesFoundOnSoundCloud: not exactly one row was updated by addWebsite for artist" +
                  artist + "for website " + normalizedWebsite)
            }
        }
        readSoundCloudWebsites(soundCloudResponse)
      }
  }
}
