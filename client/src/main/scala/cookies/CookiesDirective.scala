package cookies

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{ElementDirective, injectable}
import org.scalajs.dom._
import org.scalajs.dom.html.Html
import utilities.NgCookies

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("cookies")
class CookiesDirective(timeout: Timeout, ngCookies: NgCookies) extends ElementDirective {
  val cookies = document.getElementsByTagName("cookies").item(0).asInstanceOf[Html]
  @JSExport
  def hideCookies(): Unit = {
    ngCookies.put("hiddenCookies", true)
    cookies.innerHTML = ""
  }

  cookies.getElementsByTagName("i").item(0).asInstanceOf[Html].onclick = (event: MouseEvent) => hideCookies()

  ngCookies.get("hiddenCookies") match {
    case isHidden if isHidden.isInstanceOf[String] =>
      cookies.innerHTML = ""
  }

}