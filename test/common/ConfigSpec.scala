package common

import common.config.AppConfig
import org.scalatest.{Matchers, WordSpec}

class ConfigSpec extends WordSpec with Matchers {

  "AppConfig" should {

    "load itself from application.conf file" in {
      val config = AppConfig.load()

      config should not be (null)
    }
  }
}
