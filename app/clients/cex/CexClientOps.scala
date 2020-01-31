package clients.cex

import cats.implicits._

import domain.ItemDetails
import domain.ItemDetails.{GameDetails, PhoneDetails}

private trait QueryStringGenerator[A <: ItemDetails] {
  def generate(details: A): Option[String]
}

private[cex] object CexClientOps {
  val gameDetailsQueryStringGenerator: QueryStringGenerator[GameDetails] =
    (details: GameDetails) => details.name.flatMap(n => details.platform.map(p => s"$n $p"))

  val phoneDetailsQueryStringGenerator: QueryStringGenerator[PhoneDetails] =
    (details: PhoneDetails) => List(details.make, details.model, details.storageCapacity, details.colour, details.network).sequence.map(_.mkString(" "))

  implicit class ItemDetailsSyntax(val details: ItemDetails) {
    def searchQuery: Option[String] = details match {
      case details: GameDetails => gameDetailsQueryStringGenerator.generate(details)
      case details: PhoneDetails => phoneDetailsQueryStringGenerator.generate(details)
    }
  }
}
