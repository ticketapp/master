package models

import java.util.UUID
import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}

import scala.concurrent.Future
import scala.language.postfixOps


case class Genre (id: Option[Int], name: String, icon: Char = 'a') {
  require(name.nonEmpty, "It is forbidden to create a genre without a name.")
}

class GenreMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def formApply(name: String) = new Genre(None, name)
  def formUnapply(genre: Genre) = Some(genre.name)

  def findAll: Future[Seq[Genre]] = {
    db.run(genres.result)
  }

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

  //  def findContaining(pattern: String): Future[Seq[Genre]] = {
  //    val lowercasePattern = pattern.toLowerCase
  //    val query = for {
  //      genre <- genres if genre.name.toLowerCase like s"%$lowercasePattern%"
  //    } yield genre
  //
  //    db.run(query.take(12).result)
  //
  ////        """SELECT * FROM genres
  ////          | WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%'
  ////          | LIMIT 12""".stripMargin)
  ////        .on('patternLowCase -> pattern.toLowerCase)
  ////        .as(GenreParser.*)
  ////    }
  //  }

  /*def save(genre: Genre): Future[Genre] =
    db.run(genres returning genres.map(_.id) into ((genre, id) => genre.copy(id = Option(id))) += genre)*/

  def save(genre: Genre): Future[Genre] = db.run((for {
    genreFound <- genres.filter(_.name === genre.name).result.headOption
    result <- genreFound.map(DBIO.successful).getOrElse(genres returning genres.map(_.id) += genre)
  } yield { result match {
      case g: Genre => g
      case id: Int => genre.copy(id = Option(id))
    }
  }).transactionally)

  def findByFacebookUrl(facebookUrl: String): Future[Option[Artist]] = {
    val query = artists.filter(_.facebookUrl === facebookUrl)
    db.run(query.result.headOption)
  }
  
  def findByName(name: String): Future[Option[Genre]] = db.run(genres.filter(_.name === name).result.headOption)

  def saveWithEventRelation(genre: Genre, eventId: Long): Future[Int] = save(genre) flatMap { 
    _.id match {
      case None =>
        Logger.error("Genre.saveWithEventRelation: genre saved returned None as id")
        Future(0)
      case Some(id) =>
        saveEventRelation(EventGenreRelation(eventId, id))
    }
  }

  def saveEventRelation(eventGenreRelation: EventGenreRelation): Future[Int] =
    db.run(eventsGenres += eventGenreRelation)

  def deleteEventRelation(eventGenreRelation: EventGenreRelation): Future[Int] = db.run(eventsGenres.filter(eventGenre =>
    eventGenre.eventId === eventGenreRelation.eventId && eventGenre.genreId === eventGenreRelation.genreId).delete)

  def saveWithArtistRelation(genre: Genre, artistId: Long): Future[Int] = save(genre) flatMap {
    _.id match {
      case None =>
        Logger.error("Genre.saveWithArtistRelation: genre saved returned None as id")
        Future(0)
      case Some(id) =>
        saveArtistRelation(ArtistGenreRelation(artistId, id))
    }
  }

  def saveArtistRelation(artistGenreRelation: ArtistGenreRelation): Future[Int] =
    db.run(artistsGenres += artistGenreRelation)

  def deleteArtistRelation(artistGenreRelation: ArtistGenreRelation): Future[Int] = db.run(artistsGenres.filter(artistGenre =>
    artistGenre.artistId === artistGenreRelation.artistId && artistGenre.genreId === artistGenreRelation.genreId).delete)
  
  def saveGenreForArtistInFuture(genreName: Option[String], artistId: Long): Unit = {
//    Future {
//      genreName match {
//        case Some(genreFound) if genreFound.nonEmpty =>
//          saveWithArtistRelation(new Genre(None, genreFound), artistId)
//      }
//    }
  }
      /*
      DONE BY LOLO:
      
  def saveGenreForArtistInFuture(genreName: Option[String], artistId: Long): Unit = {
    Future {
      genreName match {
        case Some(genreFound) if genreFound.nonEmpty =>
          saveWithArtistRelation(new Genre(None, genreFound), artistId)
          val overGenre = findOverGenres(Seq(Genre(None, genreFound)))
          overGenre.foreach(genre => saveWithArtistRelation(genre, artistId))
      }
       */

  def saveWithTrackRelation(genre: Genre, trackId: UUID): Future[Int] = save(genre) flatMap {
    _.id match {
      case None =>
        Logger.error("Genre.saveWithTrackRelation: genre saved returned None as id")
        Future(0)
      case Some(id) =>
        saveTrackRelation(TrackGenreRelation(trackId, id))
    }
  }

  def findOverGenres(genres: Seq[Genre]): Future[Seq[Genre]] = Future.sequence(genres map { genre: Genre =>
    findByName(genre.name) flatMap {
      case Some(genre: Genre) =>
        findById(genre.id.getOrElse(-1)) map {
          case Some(genreFound) if genreFound.icon =='g' =>
            Option(Genre(None, "reggae"))
          case Some(genreFound) if genreFound.icon == 'e' =>
            Option(Genre(None, "electro"))
          case Some(genreFound) if genreFound.icon == 'h' =>
            Option(Genre(None, "hip-hop"))
          case Some(genreFound) if genreFound.icon == 'j' =>
            Option(Genre(None, "jazz"))
          case Some(genreFound) if genreFound.icon == 's' =>
            Option(Genre(None, "classique"))
          case Some(genreFound) if genreFound.icon == 'l' =>
            Option(Genre(None, "musiques latines"))
          case Some(genreFound) if genreFound.icon == 'r' =>
            Option(Genre(None, "rock"))
          case Some(genreFound) if genreFound.icon == 'c' =>
            Option(Genre(None, "chanson"))
          case Some(genreFound) if genreFound.icon == 'm' =>
            Option(Genre(None, "musiques du monde"))
          case Some(genreFound) if genreFound.icon == 'a' =>
            Option(Genre(None, ""))
          case None =>
            Logger.error("Artist.findOverGenres: no genre found for this id")
            None
        }
      case _ =>
        Logger.error("Artist.findOverGenres: no genre found for this name")
        Future(None)
    }
  }).map { _.flatten }

  def saveTrackRelation(trackGenreRelation: TrackGenreRelation): Future[Int] =
    db.run(tracksGenres += trackGenreRelation)

  def deleteTrackRelation(trackGenreRelation: TrackGenreRelation): Future[Int] = db.run(tracksGenres.filter(trackGenre =>
    trackGenre.trackId === trackGenreRelation.trackId && trackGenre.genreId === trackGenreRelation.genreId).delete)

  def delete(id: Int): Future[Int] = db.run(genres.filter(_.id === id).delete) 

  def followGenre(userGenreRelation: UserGenreRelation): Future[Int] =
    db.run(genresFollowed += userGenreRelation)

  def genresStringToGenresSet(genres: Option[String]): Set[Genre] = genres match {
    case None => Set.empty
    case Some(genres: String) =>
      val lowercaseGenres = genres.toLowerCase.replace("\n", "").filter(_ >= ' ')
      val genresSplitByCommas = lowercaseGenres.trim.split(",").flatMap { _.trim.split("/") }
        .flatMap { _.trim.split(":") }.flatMap { _.trim.split(";") }.filter(_.nonEmpty)
      if (genresSplitByCommas.length > 1) {
        genresSplitByCommas.map { genreName => Genre(None, genreName.stripSuffix(",").trim) }.toSet
      } else {
        """([%/+\.;]|& )""".r
          .split(lowercaseGenres)
          .map { _.trim }
          .filterNot(_ == "")
        match {
          case list if list.length != 1 =>
            list.map { genreName => new Genre(None, genreName) }.toSet
          case listOfOneItem =>
            listOfOneItem(0) match {
              case genre if genre.contains("'") || genre.contains("&") || genre.contains("musique") ||
                genre.contains("musik") || genre.contains("music") =>
                Set(new Genre(None, genre))
              case genreWithoutForbiddenChars =>
                genreWithoutForbiddenChars
                  .split("\\s+")
                  .map { genreName => new Genre(None, genreName.stripSuffix(".")) }
                  .filterNot(_.name == "")
                  .toSet
            }
        }
      }
  }
}