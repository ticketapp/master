import org.specs2.specification.core.Env
import play.api.test.PlaySpecification

class TestPostgresDockerService(envv: Env)  extends PlaySpecification with DockerTestKit with DockerPostgresService {

  implicit val ee = envv.executionEnv

  postgresContainer.isReady must beTrue.await
}
