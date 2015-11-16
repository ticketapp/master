import akka.util.Timeout
import org.specs2.mock.Mockito
import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.Play
import play.api.db.evolutions.Evolutions
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.test.PlaySpecification

import scala.concurrent._



trait GlobalApplicationForControllers extends PlaySpecification with Mockito with BeforeAll with AfterAll with Context with Injectors {

  override def beforeAll() {
    Play.start(application)
    Evolutions.applyEvolutions(databaseApi.database("tests"))
  }

  override def afterAll() {
    Evolutions.cleanupEvolutions(databaseApi.database("tests"))
    Play.stop(application)
  }

  override def contentAsBytes(of: Future[Result])(implicit timeout: Timeout): Array[Byte] = {
    val result = Await.result(of, timeout.duration)
    val eBytes = result.header.headers.get(TRANSFER_ENCODING) match {
      case Some("chunked") => result.body &> Results.dechunk
      case _ => result.body
    }

    Await.result(eBytes |>>> Iteratee.consume[Array[Byte]](), timeout.duration)
  }
}