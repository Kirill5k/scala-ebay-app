package clients.cex

import domain.ResellPrice
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.Future

class CexClient @Inject() (config: Configuration, client: WSClient) {

  def findResellPrice(query: String): Future[Either[Throwable, ResellPrice]] = {

  }
}
