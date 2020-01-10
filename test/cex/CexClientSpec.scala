package cex

import domain.ResellPrice
import exceptions.{HttpError, InternalError}
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

import scala.concurrent.duration._
import scala.language.postfixOps


class CexClientSpec extends PlaySpec with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  val cexConfig = Map("baseUri" -> "/cex", "searchPath" -> "/search")
  val config: Configuration = Configuration("cex" -> cexConfig)

  val queryString = "iphone 7"

  "CexClient" should {

    "find minimal resell price" in {
      withCexClient(200, "cex/search-success-response.json") { cexClient =>
        val result = cexClient.findResellPrice("iphone 7")

        whenReady(result.value, timeout(6 seconds), interval(500 millis)) { minPrice =>
          minPrice must be (Right(ResellPrice(BigDecimal.valueOf(108), BigDecimal.valueOf(153))))
        }
      }
    }

    "return 0 resell price when no results" in {
      withCexClient(200, "cex/search-noresults-response.json") { cexClient =>
        val result = cexClient.findResellPrice("iphone 7")

        whenReady(result.value, timeout(6 seconds), interval(500 millis)) { minPrice =>
          minPrice must be (Right(ResellPrice(BigDecimal.valueOf(0), BigDecimal.valueOf(0))))
        }
      }
    }

    "return internal error when failed to parse json" in {
      withCexClient(200, "cex/search-unexpected-response.json") { cexClient =>
        val result = cexClient.findResellPrice("iphone 7")

        whenReady(result.value, timeout(6 seconds), interval(500 millis)) { minPrice =>
          minPrice must be (Left(InternalError("error parsing json response: C[A]: DownField(boxes),DownField(data),DownField(response)")))
        }
      }
    }

    "return http error when not success" in {
      withCexClient(400, "cex/search-error-response.json") { cexClient =>
        val result = cexClient.findResellPrice("iphone 7")

        whenReady(result.value, timeout(6 seconds), interval(500 millis)) { minPrice =>
          minPrice must be (Left(HttpError(400, "error sending request to cex: Bad Request")))
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
