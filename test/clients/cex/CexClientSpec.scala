package clients.cex

import domain.{ResellPrice, VideoGameBuilder}
import domain.ApiClientError._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.MediaRange
import play.api.mvc.Results
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.core.server.Server
import play.filters.HttpFiltersComponents


class CexClientSpec extends PlaySpec with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  val cexConfig = Map("baseUri" -> "/cex", "searchPath" -> "/search")
  val config: Configuration = Configuration("cex" -> cexConfig)

  val queryString = "super mario 3 XBOX ONE"

  "CexClient" should {
    val gameDetails = VideoGameBuilder.build("super mario 3", platform = "XBOX ONE").itemDetails

    "find minimal resell price and store it in cache" in {
      withCexClient(200, "cex/search-success-response.json") { cexClient =>
        val result = cexClient.findResellPrice(gameDetails)

        whenReady(result.unsafeToFuture()) { price =>
          val expectedPrice = Some(ResellPrice(BigDecimal.valueOf(108), BigDecimal.valueOf(153)))
          price must be (expectedPrice)
          cexClient.searchResultsCache.get(queryString) must be (expectedPrice)
        }
      }
    }

    "return none when no results" in {
      withCexClient(200, "cex/search-noresults-response.json") { cexClient =>
        val result = cexClient.findResellPrice(gameDetails)

        whenReady(result.unsafeToFuture()) { price =>
          price must be (None)
          cexClient.searchResultsCache.containsKey(queryString) must be (false)
        }
      }
    }

    "return none when not enough details" in {
      withCexClient(200, "cex/search-noresults-response.json") { cexClient =>
        val result = cexClient.findResellPrice(gameDetails.copy(name = None))

        whenReady(result.unsafeToFuture()) { price =>
          price must be (None)
          cexClient.searchResultsCache.isEmpty must be (true)
        }
      }
    }

    "return internal error when failed to parse json" in {
      withCexClient(200, "cex/search-unexpected-response.json") { cexClient =>
        val result = cexClient.findResellPrice(gameDetails)

        whenReady(result.unsafeToFuture()) { price =>
          price must be (JsonParsingError("C[A]: DownField(boxes),DownField(data),DownField(response)"))
          cexClient.searchResultsCache.isEmpty must be (true)
        }
      }
    }

    "return http error when not success" in {
      withCexClient(400, "cex/search-error-response.json") { cexClient =>
        val result = cexClient.findResellPrice(gameDetails)

        whenReady(result.unsafeToFuture()) { price =>
          price must be (HttpError(400, "error sending request to cex: Bad Request"))
          cexClient.searchResultsCache.isEmpty must be (true)
        }
      }
    }

    "return none when 429 returned" in {
      withCexClient(429, "cex/search-error-response.json") { cexClient =>
        val result = cexClient.findResellPrice(gameDetails)

        whenReady(result.unsafeToFuture()) { price =>
          price must be (None)
          cexClient.searchResultsCache.isEmpty must be (true)
        }
      }
    }
  }

  def withCexClient[T](status: Int, responseFile: String)(block: CexClient => T): T = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/cex/search" ? q"q=$query") =>
            Action{req =>
              query must be (queryString)
              req.contentType must be (Some("application/json"))
              req.acceptedTypes must be (MediaRange.parse("application/json"))
              Results.Status(status).sendResource(responseFile)(executionContext, fileMimeTypes)
            }
        }
      }.application
    } { implicit port =>
      WsTestClient.withClient { client =>
        block(new CexClient(config, client))
      }
    }
  }
}
