/*
package services

import java.util.UUID
import org.mindrot.jbcrypt.BCrypt
import play.api.Play.current
import akka.actor._
import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import scala.language.{implicitConversions, postfixOps}
import scala.util.Try

class SecuredActionRequest[A](val uuid: Option[UUID], val role: Option[Int], request: Request[A])
  extends WrappedRequest[A](request)

object SecuredAction extends ActionBuilder[SecuredActionRequest] {
  def invokeBlock[A](request: Request[A], block: (SecuredActionRequest[A]) => Future[SimpleResult]) = {
    request.session.get("connected") match {
      case Some(uuid) =>
        try {
          block(new SecuredActionRequest(
            Some(UUID.fromString(uuid)), Some(request.session.get("role").getOrElse("0").toInt), request))
        } catch {
          case e:Exception =>
            Logger error "UserActor.invokeBlock" + e.getMessage
            block(new SecuredActionRequest(None, None, request))
        }
      case None =>
        block(new SecuredActionRequest(None, None, request))
    }
  }
}*/
