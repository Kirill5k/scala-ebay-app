package clients

import cats.effect.{ContextShift, IO, Resource}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import resources.SttpBackendResource
import sttp.client
import sttp.client.{NothingT, SttpBackend}
import sttp.client.testing.SttpBackendStub
import sttp.model.{Header, HeaderNames, MediaType, Method}

import scala.concurrent.ExecutionContext
import scala.io.Source

trait SttpClientSpec extends PlaySpec with ScalaFutures {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  def isGoingTo(
                 req: client.Request[_, _],
                 method: Method,
                 host: String,
                 paths: Seq[String] = Nil,
                 params: Map[String, String] = Map.empty
               ): Boolean =
    req.uri.host == host &&
      (paths.isEmpty || req.uri.path == paths) &&
      req.method == method &&
      req.uri.params.toMap.toSet[(String, String)].subsetOf(params.toSet)

  def isGoingToWithSpecificContent(
      req: client.Request[_, _],
      method: Method,
      host: String,
      paths: Seq[String] = Nil,
      params: Map[String, String] = Map.empty,
      contentType: MediaType = MediaType.ApplicationJson
  ): Boolean =
    isGoingTo(req, method, host, paths, params) &&
      req.headers.contains(Header(HeaderNames.Accept, MediaType.ApplicationJson.toString())) &&
      req.headers.contains(Header(HeaderNames.ContentType, contentType.toString()))

  def json(path: String): String = Source.fromResource(path).getLines.toList.mkString

  def sttpCatsBackend(testingBackend: SttpBackendStub[IO, Nothing]): SttpBackendResource[IO] = new SttpBackendResource[IO] {
    override val get: Resource[IO, SttpBackend[IO, Nothing, NothingT]] =
      Resource.liftF(IO.pure(testingBackend))
  }
}
