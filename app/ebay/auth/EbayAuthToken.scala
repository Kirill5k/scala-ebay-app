package ebay.auth

import java.time.Instant

private[auth] final case class EbayAuthToken(token: String, expiresAt: Instant) {
  def isValid: Boolean = expiresAt.isAfter(Instant.now())
}

private[auth] object EbayAuthToken {
  def apply(token: String, expiresIn: Long): EbayAuthToken = new EbayAuthToken(token, Instant.now().plusSeconds(expiresIn))
}
