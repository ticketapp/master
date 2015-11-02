import org.specs2.mock.Mockito
import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.Play
import play.api.db.evolutions.Evolutions
import play.api.test.PlaySpecification


trait GlobalApplicationForControllers extends PlaySpecification with Mockito with BeforeAll with AfterAll with Context with Injectors {

  override def beforeAll() {
    Play.start(application)
    Evolutions.applyEvolutions(databaseApi.database("tests"))
  }

  override def afterAll() {
    Evolutions.cleanupEvolutions(databaseApi.database("tests"))
    Play.stop(application)
  }
}