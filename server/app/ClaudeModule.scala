import application.Global
import com.google.inject.AbstractModule

class ClaudeModule extends AbstractModule {
  override def configure() = bind(classOf[Global])
}
