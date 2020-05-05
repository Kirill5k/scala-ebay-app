package resources

import cats.effect.{ContextShift, IO, Resource}
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import sttp.client.{NothingT, SttpBackend}
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.ExecutionContext


@ImplementedBy(classOf[CatsSttpBackendResource])
trait SttpBackendResource[F[_]] {
  val get: Resource[F, SttpBackend[F, Nothing, NothingT]]
}

@Singleton
final class CatsSttpBackendResource @Inject()(implicit private val ec: ExecutionContext) extends SttpBackendResource[IO] {

  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  val get: Resource[IO, SttpBackend[IO, Nothing, NothingT]] =
    Resource.make(AsyncHttpClientCatsBackend[IO]())(_.close())
}
