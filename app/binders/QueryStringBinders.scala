package binders

import java.time.{Instant, LocalDate, ZoneOffset}

import play.api.mvc.QueryStringBindable

import scala.util.{Either, Try}

object QueryStringBinders {
  implicit def bindableInstant(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[Instant] = new QueryStringBindable[Instant] {
    override def bind(key: String, params: Map[String,Seq[String]]): Option[Either[String, Instant]] =
      params.get(key)
        .flatMap(_.headOption)
        .map(parseDate)

    private def parseDate(dateString: String): Either[String, Instant] = {
      val date  = if (dateString.length == 10) Try(LocalDate.parse(dateString)).map(_.atStartOfDay(ZoneOffset.UTC).toInstant) else Try(Instant.parse(dateString))
      date.toOption.toRight(s"Invalid date format: $dateString")
    }

    override def unbind(key: String, date: Instant): String =
      s"$key=${date.toString}"
  }
}
