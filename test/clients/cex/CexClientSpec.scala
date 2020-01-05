package clients.cex

import domain.ResellPrice
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.routing.Router
import play.api.test.WsTestClient
import play.core.server.Server
import play.api.routing.sird._
import play.filters.HttpFiltersComponents


class CexClientSpec extends PlaySpec with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  val cexConfig = Map("baseUri" -> "", "searchPath" -> "/search")
  val config: Configuration = Configuration("cex" -> cexConfig)

  "CexClient" should {
    "find minimal resell price" in {
      Server.withApplicationFromContext() { context =>
        new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
          override def router: Router = Router.from {
            case GET(p"/search") =>
              Action { req =>
                Results.Ok.sendResource("cex/cex-search-response.json")(executionContext, fileMimeTypes)
              }
          }
        }.application
      } { implicit port =>
        WsTestClient.withClient { client =>
          val cexClient = new CexClient(config, client)
          val result = cexClient.findResellPrice("iphone 7")

          whenReady(result) { minPrice =>
            minPrice must be (Right(ResellPrice(BigDecimal.valueOf(10), BigDecimal.valueOf(15))))
          }
        }
      }
    }
  }
}
