package common

import play.api.Logger

trait Logging {
  val loggerName             = getClass.getName
  @transient lazy val logger = Logger(loggerName)
}
