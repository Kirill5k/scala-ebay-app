package common.cache

import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class CacheSpec extends AsyncFreeSpec with Matchers {
  implicit val cs = IO.contextShift(ExecutionContext.global)
  implicit val t  = IO.timer(ExecutionContext.global)

  "A RefCache" - {
    "add new items to cache" in {
      val result = for {
        cache <- Cache.make[IO, String, String](2 minutes, 30 seconds)
        _     <- cache.add("foo", "bar")
        res     <- cache.get("foo")
      } yield res

      result.unsafeToFuture().map(_ must be (Some("bar")))
    }

    "check if item is in cache" in {
      val result = for {
        cache <- Cache.make[IO, String, String](2 minutes, 30 seconds)
        _     <- cache.add("foo", "bar")
        res     <- cache.contains("foo")
      } yield res

      result.unsafeToFuture().map(_ must be (true))
    }

    "remove item after it has expired" in {
      val result = for {
        cache <- Cache.make[IO, String, String](3 seconds, 1 seconds)
        _     <- cache.add("foo", "bar")
        _ <- IO.sleep(5 seconds)
        res     <- cache.contains("foo")
      } yield res

      result.unsafeToFuture().map(_ must be (false))
    }
  }
}
