package domain

import cats.implicits._

sealed trait Packaging

object Packaging {
  final case object Single extends Packaging
  final case object Bundle extends Packaging
}

sealed trait ItemDetails {
  def fullName: Option[String]
}

object ItemDetails {

  final case class GenericItemDetails(
      name: String
  ) extends ItemDetails {
    val fullName: Option[String] = Some(name)
  }

  final case class PhoneDetails(
      make: Option[String],
      model: Option[String],
      colour: Option[String],
      storageCapacity: Option[String],
      network: Option[String],
      condition: Option[String]
  ) extends ItemDetails {
    val fullName: Option[String] = List(make, model, storageCapacity, colour, network).sequence.map(_.mkString(" "))
  }

  final case class GameDetails(
      name: Option[String],
      platform: Option[String],
      releaseYear: Option[String],
      genre: Option[String],
      packaging: Packaging = Packaging.Single
  ) extends ItemDetails {
    val fullName: Option[String] = for (n <- name; p <- platform) yield s"$n $p"
  }

}
