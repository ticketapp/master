import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.db.evolutions.Evolutions

trait GlobalApplicationForModels extends PlaySpec with OneAppPerSuite with Injectors with BeforeAndAfterAll {

  override def beforeAll() = {
    Evolutions.applyEvolutions(databaseApi.database("tests"))
  }

  override def afterAll() = {
    Evolutions.cleanupEvolutions(databaseApi.database("tests"))
  }

  def isOrdered(list: List[Double]): Boolean = list match {
    case Nil => true
    case x :: Nil => true
    case x :: xs => x <= xs.head && isOrdered(xs)
  }
}
