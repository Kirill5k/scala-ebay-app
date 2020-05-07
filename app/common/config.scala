package common

import pureconfig._
import pureconfig.generic.auto._

object config {

  final case class EbayCredentials(clientId: String, clientSecret: String)

  final case class EbayConfig(
      baseUri: String,
      credentials: List[EbayCredentials]
  )

  final case class CexConfig(
      baseUri: String
  )

  final case class TelegramConfig(
      baseUri: String,
      botKey: String,
      mainChannelId: String,
      secondaryChannelId: String
  )

  final case class AppConfig(
      cex: CexConfig,
      ebay: EbayConfig,
      telegram: TelegramConfig
  )

  object AppConfig {
    def load(): AppConfig = ConfigSource.default.loadOrThrow[AppConfig]
  }
}
