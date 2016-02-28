package genresDomain

import java.util.UUID
import javax.inject.Inject

import artistsDomain.Artist
import database.MyPostgresDriver.api._
import database._
import eventsDomain.Event
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import services.LoggerHelper

import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal


case class Genre (id: Option[Int] = None, name: String, icon: Char = 'a') {
  require(name.nonEmpty, "It is forbidden to create a genre without a name.")
}

case class GenreWithWeight(genre: Genre, weight: Int = 1)


class GenreMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions with LoggerHelper {

  def findAll: Future[Seq[Genre]] = db.run(genres.result)

  def findById(id: Int): Future[Option[Genre]] = {
    val query = genres.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def findAllByEvent(event: Event): Future[Seq[Genre]] = {
    val query = for {
      e <- events if e.id === event.id
      eventGenre <- eventsGenres
      genre <- genres if genre.id === eventGenre.genreId
    } yield genre

    db.run(query.result)
  }

  def findAllByArtist(artist: Artist): Future[Seq[Genre]] = {
    val query = for {
      a <- artists if a.id === artist.id
      artistGenre <- artistsGenres
      genre <- genres if genre.id === artistGenre.genreId
    } yield genre

    db.run(query.result)
  }
 
  def findAllByTrack(trackId: UUID): Future[Seq[Genre]] = {
    val query = for {
      track <- tracks if track.uuid === trackId
      trackGenre <- tracksGenres
      genre <- genres if genre.id === trackGenre.genreId
    } yield genre

    db.run(query.result)
  }

  def findAllContaining(pattern: String): Future[Seq[Genre]] = {
    val lowercasePattern = pattern.toLowerCase

    val query = for {
      (genre) <- genres
      if genre.name.toLowerCase like s"%$lowercasePattern%"
    } yield genre

    db.run(query.result)
  }

  def save(genre: Genre): Future[Genre] = db.run((for {
    genreFound <- genres.filter(_.name === genre.name).result.headOption
    result <- genreFound.map(DBIO.successful).getOrElse(genres returning genres.map(_.id) +=
      genre.copy(name = genre.name.toLowerCase))
  } yield result match {
    case g: Genre => g
    case id: Int => genre.copy(id = Option(id))
  }).transactionally)
  
  def findByName(name: String): Future[Option[Genre]] =
    db.run(genres.filter(_.name === name.toLowerCase).result.headOption)
    
  def findSequenceByName(names: Seq[String]): Future[Seq[Genre]] = {
    val lowerCaseNames = names map (_.toLowerCase)
    
    val query = for {
      genre <- genres
      if genre.name inSetBind lowerCaseNames
    } yield genre
    
    db.run(query.result)
  }

  def isAGenre(pattern: String): Future[Boolean] = db.run(genres.filter(_.name === pattern).exists.result)

  def saveWithEventRelation(genre: Genre, eventId: Long): Future[Int] = save(genre) flatMap {
    _.id match {
      case None =>
        log("Genre.saveWithEventRelation: genre saved returned None as id")
        Future(0)

      case Some(id) =>
        saveEventRelation(EventGenreRelation(eventId, id))
    }
  }

  def saveEventRelation(eventGenreRelation: EventGenreRelation): Future[Int] =
    db.run(eventsGenres += eventGenreRelation) recover { case NonFatal(e) =>
      log(s"The relation genre-event $eventGenreRelation was not saved", e)
      0
    }

  def saveEventRelations(eventGenreRelations: Seq[EventGenreRelation]): Future[Boolean] =
    db.run(eventsGenres ++= eventGenreRelations) map { _ =>
      true
    } recover {
      case e: Exception =>
        log(e)
        false
    }

  def deleteEventRelation(eventGenreRelation: EventGenreRelation): Future[Int] = db.run(eventsGenres.filter(eventGenre =>
    eventGenre.eventId === eventGenreRelation.eventId && eventGenre.genreId === eventGenreRelation.genreId).delete)

  def saveWithArtistRelation(genre: Genre, artistId: Long): Future[Option[ArtistGenreRelation]] = save(genre) flatMap {
    _.id match {
      case None =>
        log("Genre.saveWithArtistRelation: genre saved returned None as id")
        Future(None)
      case Some(id) =>
        saveArtistRelation(ArtistGenreRelation(artistId, id)) map { Option(_) }
    }
  }

  def saveArtistRelation(artistGenreRelation: ArtistGenreRelation): Future[ArtistGenreRelation] = {
    db.run((for {
      artistGenreFound <- artistsGenres.filter(relation => relation.artistId === artistGenreRelation.artistId &&
        relation.genreId === artistGenreRelation.genreId).result.headOption
      result <- artistGenreFound.map(DBIO.successful).getOrElse(artistsGenres returning artistsGenres.map(_.artistId) += artistGenreRelation)
    } yield result match {
      case artistGenre: ArtistGenreRelation =>
        val updatedArtistGenreRelation = artistGenre.copy(weight = artistGenre.weight + 1)
        updateArtistRelation(updatedArtistGenreRelation) map {
          case int if int != 1 =>
            log("Genre.saveArtistRelation: not exactly one row was updated")
            artistGenre
          case _ =>
            updatedArtistGenreRelation
        }
      case id: Long => Future(artistGenreRelation.copy(artistId = id))
    }).transactionally) flatMap { maybeRelation => maybeRelation }
  }

  def updateArtistRelation(artistGenreRelation: ArtistGenreRelation): Future[Int] =
    db.run(artistsGenres.filter(relation => relation.artistId === artistGenreRelation.artistId &&
      relation.genreId === artistGenreRelation.genreId).update(artistGenreRelation))

  def deleteArtistRelation(artistGenreRelation: ArtistGenreRelation): Future[Int] = db.run(artistsGenres.filter(artistGenre =>
    artistGenre.artistId === artistGenreRelation.artistId && artistGenre.genreId === artistGenreRelation.genreId).delete)

  def saveGenreOfArtist(genreName: String, artistId: Long): Unit = {
    saveWithArtistRelation(new Genre(None, genreName), artistId)

    findOverGenres(Seq(Genre(None, genreName))) map { _.foreach(genre => saveWithArtistRelation(genre, artistId)) }
  }

  def saveMaybeGenreOfArtist(maybeGenreName: Option[String], artistId: Long): Unit = maybeGenreName match {
    case Some(genre) => saveGenreOfArtist(genre, artistId)
    case _ =>
  }

  // not used
  def saveWithTrackRelation(genre: Genre, trackId: UUID): Future[Int] = save(genre) flatMap {
    _.id match {
      case None =>
        log("Genre.saveWithTrackRelation: genre saved returned None as id")
        Future(0)
      case Some(id) =>
        saveTrackRelation(TrackGenreRelation(trackId, id))
    }
  }
  //

  def findOverGenres(genres: Seq[Genre]): Future[Seq[Genre]] = {
    val overGenres = findSequenceByName(genres map(_.name))

    overGenres map { genres =>
      genres collect {
        case genreFound if genreFound.icon == 'g' =>
          Genre(None, "reggae")
        case genreFound if genreFound.icon == 'e' =>
          Genre(None, "electro")
        case genreFound if genreFound.icon == 'h' =>
          Genre(None, "hip-hop")
        case genreFound if genreFound.icon == 'j' =>
          Genre(None, "jazz")
        case genreFound if genreFound.icon == 's' =>
          Genre(None, "classique")
        case genreFound if genreFound.icon == 'l' =>
          Genre(None, "musiques latines")
        case genreFound if genreFound.icon == 'r' =>
          Genre(None, "rock")
        case genreFound if genreFound.icon == 'c' =>
          Genre(None, "chanson")
        case genreFound if genreFound.icon == 'm' =>
          Genre(None, "musiques du monde")
      }
    }
  }

  def saveTrackRelation(trackGenreRelation: TrackGenreRelation): Future[Int] =
    db.run(tracksGenres += trackGenreRelation)

  def deleteTrackRelation(trackGenreRelation: TrackGenreRelation): Future[Int] = db.run(tracksGenres.filter(trackGenre =>
    trackGenre.trackId === trackGenreRelation.trackId && trackGenre.genreId === trackGenreRelation.genreId).delete)

  def delete(id: Int): Future[Int] = db.run(genres.filter(_.id === id).delete) 

  def genresStringToGenresSet(genres: String): Set[Genre] = {
    val refactoredGenres = genres
      .toLowerCase
      .filter(_ >= ' ')
      .replaceAll("'", "")
      .replaceAll("&", "")
      .replaceAll("musique", "")
      .replaceAll("musik", "")
      .replaceAll("music", "")
    val genresSplitBySpecialCharacters = refactoredGenres
      .split(",")
      .flatMap(_.split("/"))
      .flatMap(_.split(":"))
      .flatMap(_.split(";"))
      .map(_.trim)
      .filter(_.nonEmpty)

    genresSplitBySpecialCharacters.length match {
      case twoOrPlusElement if twoOrPlusElement > 1 =>
        genresSplitBySpecialCharacters.map { genreName => Genre(None, genreName) }.toSet
      case _ =>
        """([%/+\.;]|& )""".r
          .split(refactoredGenres)
          .filterNot(_ == "") match {
          case list if list.length != 1 =>
            list.map { genreName => new Genre(None, genreName) }.toSet
          case listOfOneItem =>
            listOfOneItem(0)
              .split("\\s+")
              .filter(_.nonEmpty)
              .map { genreName => new Genre(None, genreName.stripSuffix(".")) }
              .toSet
      }
    }
  }
}