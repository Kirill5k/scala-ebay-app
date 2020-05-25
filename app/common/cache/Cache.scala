package common.cache

import java.time.Instant

import cats.effect.{Concurrent, Sync, Timer}
import cats.effect.concurrent.Ref
import cats.implicits._

import scala.concurrent.duration.FiniteDuration

sealed trait Cache[F[_], K, V] {
  def add(key: K, value: V): F[Unit]
  def contains(key: K): F[Boolean]
  def get(key: K): F[Option[V]]
}

final private class RefCache[F[_]: Sync, K, V](
    private val state: Ref[F, Map[K, (Instant, V)]]
) extends Cache[F, K, V] {

  override def add(key: K, value: V): F[Unit] =
    state.update(s => (s + (key -> (Instant.now(), value))))

  override def contains(key: K): F[Boolean] =
    state.get.map(_.contains(key))

  override def get(key: K): F[Option[V]] =
    state.get.map(_.get(key).map(_._2))
}

object Cache {
  def make[F[_]: Concurrent: Timer, K, V](
      expiresIn: FiniteDuration,
      checkOnEvery: FiniteDuration
  ): F[Cache[F, K, V]] = {
    def runExpiration(state: Ref[F, Map[K, (Instant, V)]]): F[Unit] = {
      val process = state.get.map(_.filter {
        case (_, (exp, _)) => exp.isAfter(Instant.now.minusNanos(expiresIn.toNanos))
      }).flatTap(state.set)
      Timer[F].sleep(checkOnEvery) >> process >> runExpiration(state)
    }

    Ref.of[F, Map[K, (Instant, V)]](Map.empty)
      .flatTap(s => Concurrent[F].start(runExpiration(s)).void)
      .map(ref => new RefCache[F, K, V](ref))
  }
}
