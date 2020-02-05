package domain

import cats.implicits._

sealed trait ItemDetails {
  def summary: Option[String]
}

object ItemDetails {

  final case class PhoneDetails(
                                 make: Option[String],
                                 model: Option[String],
                                 colour: Option[String],
                                 storageCapacity: Option[String],
                                 network: Option[String],
                                 condition: Option[String]
                               ) extends ItemDetails {
    val summary: Option[String] = List(make, model, storageCapacity, colour, network).sequence.map(_.mkString(" "))
  }

  final case class GameDetails(
                                name: Option[String],
                                platform: Option[String],
                                releaseYear: Option[String],
                                genre: Option[String]
                              ) extends ItemDetails {
    val summary: Option[String] = name.flatMap(n => platform.map(p => s"$n $p"))
  }

}