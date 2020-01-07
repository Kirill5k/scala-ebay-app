package configs

import java.net.URI

import com.typesafe.config.Config
import play.api.ConfigLoader

case class EbayCredentials(clientId: String, clientSecret: String)

case class EbayConfig(baseUri: URI, authPath: String, searchPath: String, itemPath: String, credentials: Array[EbayCredentials])

object EbayConfig {
  implicit val configLoader: ConfigLoader[EbayConfig] = (rootConfig: Config, path: String) => {
    import scala.jdk.CollectionConverters._
    val config = rootConfig.getConfig(path)

    val baseUri = new URI(config.getString("baseUri"))
    val credentials = config.getConfigList("credentials").asScala
      .map(creds => EbayCredentials(creds.getString("clientId"), creds.getString("clientSecret")))
      .toArray

    EbayConfig(baseUri, config.getString("authPath"), config.getString("searchPath"), config.getString("itemPath"), credentials)
  }
}
