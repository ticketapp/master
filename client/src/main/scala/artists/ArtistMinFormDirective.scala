package artists

import com.greencatsoft.angularjs.{ElementDirective, TemplatedDirective, injectable}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("artistMinForm")
class ArtistMinFormDirective() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/artists/artistMinForm.html"
}