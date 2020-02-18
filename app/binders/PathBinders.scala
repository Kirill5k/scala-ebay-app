package binders

import java.time.Instant

import play.api.mvc.PathBindable

import scala.util.{Either, Try}

object PathBinders {

  implicit def bindableInstant(implicit stringBinder: PathBindable[String]): PathBindable[Instant] = new PathBindable[Instant] {

    override def bind(key: String, value: String): Either[String, Instant] =
      for {
        dateString <- stringBinder.bind(key, value)
        date <- Try(Instant.parse(dateString)).toOption.toRight(s"Invalid date format: $dateString")
      } yield date

    override def unbind(key: String, date: Instant): String =
      s"$key=${date.toString}"
  }
}
