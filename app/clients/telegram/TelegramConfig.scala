package clients.telegram

import java.net.URI

import com.typesafe.config.Config
import play.api.ConfigLoader

final case class TelegramConfig(baseUri: URI, messagePath: String, mainChannelId: String, secondaryChannelId: String)

object TelegramConfig {
  implicit val configLoader: ConfigLoader[TelegramConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    TelegramConfig(new URI(config.getString("baseUri")), config.getString("messagePath"), config.getString("mainChannelId"), config.getString("secondaryChannelId"))
  }
}


