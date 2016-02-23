package auth

import com.greencatsoft.angularjs.core.{HttpService, Promise, Timeout}
import com.greencatsoft.angularjs.{Attributes, ElementDirective, injectable}
import org.scalajs.dom.Element
import org.scalajs.dom.html.Html
import org.scalajs.dom.raw.MouseEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("auth")
class AuthDirective(timeout: Timeout, auth: Auth, httpService: HttpService) extends ElementDirective {

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
    elements.map(_.asInstanceOf[Html]).foreach { element =>
    val iconElement: Html = element.getElementsByTagName("i").item(0).asInstanceOf[Html]

      def changeIconToLogout: Unit = {
        iconElement.classList.remove("fa-facebook")
        iconElement.classList.add("fa-sign-out")
      }

      def loginCallback: scala.scalajs.js.Function1[scala.scalajs.js.Any,scala.scalajs.js.Any] = {
        (token: scala.scalajs.js.Any) => {
          changeIconToLogout
          element.onclick = logoutEvent()
          token
        }
      }

      def loginEvent(): js.Function1[MouseEvent, Promise[js.Any]] =
        (event: MouseEvent) => auth.authenticate("claude").then(loginCallback)

      def logoutEvent(): js.Function1[MouseEvent, Promise[js.Any]] =
        (event: MouseEvent) => {
          auth.logout()
          httpService.get[js.Any]("/signOut").then(logoutCallback)
        }

      def changeIconToFacebook: Unit = {
        iconElement.classList.remove("fa-sign-out")
        iconElement.classList.add("fa-facebook")
      }

      def logoutCallback: scala.scalajs.js.Function1[scala.scalajs.js.Any,scala.scalajs.js.Any] = {
        (token: scala.scalajs.js.Any) => {
          changeIconToFacebook
          element.onclick = loginEvent()
          token
        }
      }

      auth.isAuthenticated() match {
        case true =>
          changeIconToLogout
          element.onclick = logoutEvent()
        case false =>
          element.onclick = loginEvent()
      }
    }
  }
}