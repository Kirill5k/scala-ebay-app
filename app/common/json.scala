package common

import domain.Packaging
import io.circe.{Encoder, Json}
import io.circe.generic.extras.Configuration

object json {

  implicit val typeDiscriminator: Configuration =
    Configuration.default.withDiscriminator("_type")

  implicit val packagingEncoder: Encoder[Packaging] = {
    case Packaging.Single => Json.fromString("single")
    case Packaging.Bundle => Json.fromString("bundle")
  }
}
