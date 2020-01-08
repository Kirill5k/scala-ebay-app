package cex

import java.net.URI

import com.typesafe.config.Config
import play.api.ConfigLoader

case class CexConfig(baseUri: URI, searchPath: String)

object CexConfig {
  implicit val configLoader: ConfigLoader[CexConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    CexConfig(new URI(config.getString("baseUri")), config.getString("searchPath"))
  }
}
