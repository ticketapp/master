import com.google.inject.AbstractModule
import controllers.Global

class ClaudeModule extends AbstractModule {
  override def configure() = bind(classOf[Global])
}
