import models.{ArtistMethods, TrackMethods, GenreMethods, Genre}
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}

class TestGenresStringToSet extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities()
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTracks = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTracks,
    trackMethods, utilities)

  "A sequence of genres as a string" must {

    "return an empty set for an empty string" in {
      genreMethods.genresStringToGenresSet("") mustBe Set.empty
    }

    "return a unique low case genre for a single word without punctuation" in {
      val genres = List("rock", "Rap")

      val genresSets: Set[Genre] = genres.flatMap{ genreMethods.genresStringToGenresSet }.toSet

      val expectedResult = Set(Genre(None, "rock"), Genre(None, "rap"))

      genresSets mustBe expectedResult
    }

    "return genres split by comas in the string given" in {
      val genres = List("Rock, rockstep", "Hi-tech soul, Cosmic jazz-funk, Riot disco, Timeless electro")

      val genresSets: Set[Genre] = genres.flatMap { genreMethods.genresStringToGenresSet }.toSet

      val expectedResult = Set(Genre(None, "rock"), Genre(None, "rockstep"), Genre(None, "hi-tech soul"),
        Genre(None, "cosmic jazz-funk"), Genre(None, "riot disco"), Genre(None, "timeless electro"))

      genresSets should contain theSameElementsAs expectedResult
    }

    "return genres split by spaces in the string given" in {
      val genres = List("Rock Pop Covers", "Hi-tech soul jazz-funk")

      val genresSets: List[Set[Genre]] = genres.map { genreMethods.genresStringToGenresSet }

      val expectedResult = List(Set(Genre(None, "rock"), Genre(None, "pop"), Genre(None, "covers")),
        Set(Genre(None, "hi-tech"), Genre(None, "soul"), Genre(None, "jazz-funk")))

      genresSets should contain theSameElementsAs expectedResult
    }

    "return hip-hop" in {
      val genres = List("Hip-Hop", "Indie Pop-Folk")

      val genresSets: List[Set[Genre]] = genres.map { genreMethods.genresStringToGenresSet }

      val expectedResult = List(Set(Genre(None, "hip-hop")), Set(Genre(None, "indie"), Genre(None, "pop-folk")))

      genresSets should contain theSameElementsAs expectedResult
    }

    "return the genres without music or synonyms" in {
      val genres = "Electronic Dance Music"

      val expectedResult = Set(Genre(None, "electronic"), Genre(None, "dance"))

      genreMethods.genresStringToGenresSet(genres) should contain theSameElementsAs expectedResult
    }
  }
}
/*
    Some("Симфо Саунд"),
   Some("""Rock ´n Roll"""),  Some("Indie Pop-Folk"),
   Some("Electronic, Techno, Experimental"), Some("Dub"), Some("Psychedelic Roots"),
   Some("Jungle / Ragga Jungle / DnB / Drumstep / Dubstep? Hip-Hop Instrumentals / Reggae Riddims"),
   Some("Dub-Rock Reggae"),   Some("bugged out dubby soul reggae hip hop good vibes n tings"),
   Some("[ Echolot Dub System ] Dub - Reggae - Roots - Steppers - Jungle"),
   Some("bugged out dubby soul reggae hip hop good vibes n tings"), Some("House/Disco"),
   Some("Blues/Rock, American Funk,  Soul"), Some("Alternative / Pop Punk / Post Punk"), Some("Rock"),
   Some("Authentic Rock"), Some("Male"), Some("Rock Classic"), Some("Hardcore Punk'n'Roll"),
   Some("Top 40"), Some("Rock"), Some(" Rock / Ελληνικό Rock"), Some("rock/pop"), Some("팝,락,모던"),
   Some("Rocking Faces"), Some("ROCK"), Some("Blues, Rock'N'Roll, Funk, Pop/Rock"), Some("""COVERS ("Rock/pop")"""),
   Some("Rock, Hard Rock, Alternative Rock, Soft Rock, Progressive Rock, Punk, Metal, Alternative Metal, Soft Metal"),
   Some("POP DANCE"), Some("Rock, classic and alternative rock"),,Some("Hardcore, punk, blues"),
   Some("Metalcore, Hardcore"), Some("Rock, rockstep"), Some("Rock "), Some("Pop, Rock, Funk, Blues, Jazz"), Some("Rock"),
   Some("Solid Mush"), Some("Rock- Power- Pop."), Some("Rock & Blues"), Some("Rap/RnB"), Some("electro"),
   Some("A fusion of Tropical/Trance/House "), Some("Pop/Electro"), Some("Pop/Soul"), Some("Tribute Pink Floyd"),
   Some("allsorts - we like to make things sound fresh-er not fresh, fresh-eeer lol"), Some("DiscoHousePunk"),
   Some("Bass"), Some("Jazz, Bossa Nova, Classic Rock, Pop / Rock."), Some("pop- en wereldmuziek"),
   Some("Rock,Post - hardcore,pop - powercore "), Some("Rap"), Some("Entertainment"),
   Some("Wind Ensemble - Entertainment"),
   Some("Wide Variety- From Smooth Jazz to classical Jazz, from Old School to New School "),
   Some("Musical Theater, Movies, Vocal Music"),
   Some("A mix of post-rock, ambient, post-prog, and space-rock."),
   Some("Soirée fetichiste"),
   Some("""UK Garage, House N Garage, Bass Music, Jackin House, ("2 Step, 4 to the floor/4x4")  and Bassline from the past, present and future..."""))
*/

