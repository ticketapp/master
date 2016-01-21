package AdminClient

import com.greencatsoft.angularjs.core.Scope
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("contactController")
class AdminController(scope: Scope)
  extends AbstractController[Scope](scope) {

}
