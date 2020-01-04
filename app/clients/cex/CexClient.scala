package clients.cex

import configs.CexConfig
import domain.ResellPrice
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.Future

class CexClient @Inject() (config: Configuration, client: WSClient) {

  private var cexConfig = config.get[CexConfig]("cex")
  private var searchRequest = client
    .url(s"${cexConfig.baseUri}${cexConfig.searchPath}")
    .addHttpHeaders("Accept" -> "application/json")
    .addHttpHeaders("Content-Type" -> "application/json")

  def findResellPrice(query: String): Future[Either[Throwable, ResellPrice]] = ???
}
