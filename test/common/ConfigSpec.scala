package common

import common.config.AppConfig
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfigSpec extends AnyWordSpec with Matchers {

  "AppConfig" should {

    "load itself from application.conf file" in {
      val config = AppConfig.load()

      config must not be (null)
    }
  }
}
