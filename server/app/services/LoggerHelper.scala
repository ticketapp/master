package services

import play.api.Logger


trait LoggerHelper {

  def logException(exception: Exception) = {
    Logger.error(
      exception.getStackTrace.apply(1).getFileName + " " + exception.getStackTrace.apply(1).getMethodName,
      exception)
  }
}
