package ebay.search

import java.time.Instant

import exceptions.ApiClientError
import io.circe.generic.auto._
import io.circe.parser._
import play.api.libs.ws.BodyReadable

final case class ItemProperty(name: String, value: String)

final case class ItemSeller(username: String, feedbackPercentage: String, feedbackScore: Int)

final case class ItemImage(imageUrl: String)

final case class ItemPrice(value: String, currency: String)

final case class EbayItem(
                     itemId: String,
                     title: String,
                     shortDescription: String,
                     description: String,
                     categoryPath: String,
                     price: ItemPrice,
                     condition: String,
                     image: ItemImage,
                     seller: ItemSeller,
                     localizedAspects: Seq[ItemProperty],
                     buyingOptions: Seq[String],
                     itemWebUrl: String,
                     color: Option[String],
                     brand: Option[String],
                     mpn: Option[String],
                     itemEndDate: Option[Instant]
                   )

object EbayItem {
  implicit val ebayItemBodyReadable = BodyReadable[Either[ApiClientError, EbayItem]] { response =>
    import play.shaded.ahc.org.asynchttpclient.{Response => AHCResponse}
    val responseString = response.underlying[AHCResponse].getResponseBody
    decode[EbayItem](responseString).left.map(ApiClientError.jsonParsingError)
  }
}
