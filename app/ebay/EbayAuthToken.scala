package ebay

import java.time.Instant

case class EbayAuthToken(token: String, expiresAt: Instant) {
  def isValid: Boolean = expiresAt.isAfter(Instant.now())
}

object EbayAuthToken {
  def apply(token: String, expiresIn: Long): EbayAuthToken = new EbayAuthToken(token, Instant.now().plusSeconds(expiresIn))
}
