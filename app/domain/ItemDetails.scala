package domain

import cats.implicits._

sealed trait ItemDetailsType

object ItemDetailsType {
  final case object Single extends ItemDetailsType
  final case object Bundle extends ItemDetailsType
}

sealed trait ItemDetails {
  def detailsType: ItemDetailsType
  def summary: Option[String]
}

object ItemDetails {

  final case class PhoneDetails(
    make: Option[String],
    model: Option[String],
    colour: Option[String],
    storageCapacity: Option[String],
    network: Option[String],
    condition: Option[String],
    detailsType: ItemDetailsType = ItemDetailsType.Single) extends ItemDetails {
    val summary: Option[String] = List(make, model, storageCapacity, colour, network).sequence.map(_.mkString(" "))
  }

  final case class GameDetails(
    name: Option[String],
    platform: Option[String],
    releaseYear: Option[String],
    genre: Option[String],
    detailsType: ItemDetailsType = ItemDetailsType.Single) extends ItemDetails {
    val summary: Option[String] = name.flatMap(n => platform.map(p => s"$n $p"))
  }

}