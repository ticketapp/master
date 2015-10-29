import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.Play
import play.api.db.evolutions.Evolutions


trait GlobalApplication extends BeforeAll with AfterAll with Context with Injectors {

  override def beforeAll() {
    Play.start(application)
    Evolutions.applyEvolutions(databaseApi.database("tests"))
  }

  override def afterAll() {
    Evolutions.cleanupEvolutions(databaseApi.database("tests"))
    Play.stop(application)
  }
}