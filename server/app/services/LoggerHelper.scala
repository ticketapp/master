package services

import play.api.Logger

trait LoggerHelper {

  def log(message: String, throwable: Throwable)(implicit line: sourcecode.Line, file: sourcecode.File) = {
    val className = file.value.drop(file.value.lastIndexOf("/") + 1).stripSuffix(".scala")

    Logger.logger.error(className + ":" + line.value + ": " + message + ": ", throwable)
  }

  def log(message: String, exception: Exception)(implicit line: sourcecode.Line, file: sourcecode.File) = {
    val className = file.value.drop(file.value.lastIndexOf("/") + 1).stripSuffix(".scala")

    Logger.logger.error(className + ":" + line.value + ": " + message + ": ", exception)
  }

  def log(exception: Exception)(implicit line: sourcecode.Line, file: sourcecode.File) = {
    val className = file.value.drop(file.value.lastIndexOf("/") + 1).stripSuffix(".scala")

    Logger.logger.error(className + ":" + line.value + ": ", exception)
  }

  def log(message: String)(implicit line: sourcecode.Line, file: sourcecode.File) = {
    val className = file.value.drop(file.value.lastIndexOf("/") + 1).stripSuffix(".scala")

    Logger.logger.error(className + ":" + line.value + ": " + message)
  }

  def log(throwable: Throwable)(implicit line: sourcecode.Line, file: sourcecode.File) = {
    val className = file.value.drop(file.value.lastIndexOf("/") + 1).stripSuffix(".scala")

    Logger.logger.error(className + ":" + line.value + ": ", throwable)
  }
}
