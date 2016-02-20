package services

import play.api.Logger

trait LoggerHelper {

  def log(message: String = "", maybeException: Option[Exception] = None, maybeThrowable: Option[Throwable] = None)
         (implicit line: sourcecode.Line, file: sourcecode.File) = {
    val className = file.value.drop(file.value.lastIndexOf("/") + 1).stripSuffix(".scala")

    maybeThrowable match {
      case Some(t) =>
        Logger.logger.error(className + ":" + line.value + ": " + message + ": ", t)

      case _ =>
        maybeException match {
          case Some(e) => Logger.logger.error(className + ":" + line.value + ": " + message + ": ", e)
          case _ => Logger.logger.info(className + ":" + line.value  + ": " + message)
        }
    }
  }
}
