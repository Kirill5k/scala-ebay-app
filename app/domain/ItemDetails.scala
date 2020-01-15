package domain

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