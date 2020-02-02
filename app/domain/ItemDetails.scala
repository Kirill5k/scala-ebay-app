package domain

import cats.implicits._
import domain.ItemDetails.{GameDetails, PhoneDetails}

sealed trait ItemDetails

object ItemDetails {

  final case class PhoneDetails(
                                 make: Option[String],
                                 model: Option[String],
                                 colour: Option[String],
                                 storageCapacity: Option[String],
                                 network: Option[String],
                                 condition: Option[String]
                               ) extends ItemDetails

  final case class GameDetails(
                                name: Option[String],
                                platform: Option[String],
                                releaseYear: Option[String],
                                genre: Option[String]
                              ) extends ItemDetails

}

private[domain] trait SummaryGenerator[A <: ItemDetails] {
  def generate(details: A): Option[String]
}

object ItemDetailsOps {
  val gameDetailsQueryStringGenerator: SummaryGenerator[GameDetails] =
    (details: GameDetails) => details.name.flatMap(n => details.platform.map(p => s"$n $p"))

  val phoneDetailsQueryStringGenerator: SummaryGenerator[PhoneDetails] =
    (details: PhoneDetails) => List(details.make, details.model, details.storageCapacity, details.colour, details.network).sequence.map(_.mkString(" "))

  implicit class ItemDetailsSyntax(val details: ItemDetails) extends AnyVal {
    def summary: Option[String] = details match {
      case details: GameDetails => gameDetailsQueryStringGenerator.generate(details)
      case details: PhoneDetails => phoneDetailsQueryStringGenerator.generate(details)
    }
  }
}