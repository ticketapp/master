package testsHelper

import java.util.UUID

import database.MyPostgresDriver.api._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.db.evolutions.Evolutions

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

trait GlobalApplicationForModels extends PlaySpec with OneAppPerSuite with Injectors with BeforeAndAfterAll {

  implicit val actorTimeout: akka.util.Timeout = 5.seconds

  val defaultUserUUID =  UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")

  override def beforeAll() = {
    Evolutions.applyEvolutions(databaseApi.database("tests"))
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO users(userID, email) VALUES ('077f3ea6-2272-4457-a47e-9e9111108e44', 'user@facebook.com');"""),
      5.seconds)
  }

  override def afterAll() = Evolutions.cleanupEvolutions(databaseApi.database("tests"))

  def isOrdered(list: List[Double]): Boolean = list match {
    case Nil => true
    case x :: Nil => true
    case x :: xs => x <= xs.head && isOrdered(xs)
  }
}
