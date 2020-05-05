package resources

import cats.effect.{ContextShift, IO, Resource}
import javax.inject.{Inject, Singleton}
import sttp.client.{NothingT, SttpBackend}
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.ExecutionContext

@Singleton
final class CatsSttpBackend @Inject()(implicit private val ec: ExecutionContext) {

  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  val backendResource: Resource[IO, SttpBackend[IO, Nothing, NothingT]] =
    Resource.make(AsyncHttpClientCatsBackend[IO]())(_.close())
}
