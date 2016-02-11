package services

import play.api.Logger

trait LoggerHelper {

  def log(maybeMessage: Option[String] = None, maybeException: Option[Exception] = None)
         (implicit line: sourcecode.Line, file: sourcecode.File) = {
    val className = file.value.drop(file.value.lastIndexOf("/") + 1).stripSuffix(".scala")
    val message = maybeMessage.getOrElse("")

    Logger("access").logger.error("accessLog")

    maybeException match {
      case Some(e) => Logger.logger.error(className + ":" + line.value + message + ": ", e)
      case _ => Logger.logger.info(className + ":" + line.value + " " + message)
    }
  }
}
