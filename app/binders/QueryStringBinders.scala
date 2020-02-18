package binders

import java.time.Instant

import play.api.mvc.{QueryStringBindable}

import scala.util.{Either, Try}

object QueryStringBinders {
  implicit def bindableInstant(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[Instant] = new QueryStringBindable[Instant] {
    override def bind(key: String, params: Map[String,Seq[String]]): Option[Either[String, Instant]] = {
      params.get(key)
        .flatMap(_.headOption)
        .map(dateString => Try(Instant.parse(dateString)).toOption.toRight(s"Invalid date format: $dateString"))
    }

    override def unbind(key: String, date: Instant): String =
      s"$key=${date.toString}"
  }
}
