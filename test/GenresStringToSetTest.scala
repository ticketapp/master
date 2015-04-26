import models.Genre

import scala.collection.mutable
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import models.Genre.genresStringToGenresSet

class GenresStringToSetTest extends PlaySpec {

  "A sequence of genres as a string" must {

    "return an empty set for None" in {
      val genres = List(None, None)

      val genresSets: List[Set[Genre]] = genres.map { genresStringToGenresSet }

      val expectedResult = List(Set.empty, Set.empty)

      genresSets mustBe expectedResult
    }

    "return a unique low case genre for a single word without punctuation" in {
      val genres = List(Some("rock"), Some("Rap"))

      val genresSets: List[Set[Genre]] = genres.map { genresStringToGenresSet }

      val expectedResult = List(Set(Genre(None, "rock")), Set(Genre(None, "rap")))

      genresSets mustBe expectedResult
    }

    "return genres split by comas in the string given" in {
      val genres = List(Some("Rock, rockstep")/*, Some("Hi-tech soul, Cosmic jazz-funk, Riot disco, Timeless electro"),
        Some("Blues, Rock'N'Roll, Funk, Pop/Rock")*/)

      val genresSets: List[Set[Genre]] = genres.map { genresStringToGenresSet }

      val expectedResult = List(Set(Genre(None, "rock"), Genre(None, "rockstep")), Set(Genre(None, "hi-tech soul"),
        Genre(None, "cosmic jazz-funk"), Genre(None, "riot disco"), Genre(None, "Timeless electro"))
      )

      genresSets mustBe expectedResult
    }
  }
}
/*
   Some("Dubstep"), Some("Bass Music Dubstep"),
   Some("""Alternative * Alternative  is usually characterized by bands who have a "do-it-yourself" attitude"""),
   Some("rock"), Some("Electronic Dance Music"), Some("Hip-Hop"), Some("Симфо Саунд"), Some("POP PUNK"),
   Some("""Rock ´n Roll"""),  Some("Indie Pop-Folk"), Some("Pop Punk"), Some("Dub"),
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
   Some("POP DANCE"), Some("Rock, classic and alternative rock"),Some("Rock Pop Covers"),Some("Hardcore, punk, blues"),
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

//val expectedResult: Seq[Set[Genre]] = Seq(Set.empty, Set(Genre(None, "hi-tech soul"),
//  Genre(None, "cosmic jazz-funk"), Genre(None, "riot disco"), Genre(None, "timeless electro")),
//  Set(Genre(None, "circus")), Set(Genre(None, "bass"), Genre(None, "dubstep")), Set.empty
//)