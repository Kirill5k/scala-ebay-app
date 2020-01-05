package domain


case class ResellPrice(cash: BigDecimal, exchange: BigDecimal)

object ResellPrice {
  def empty(): ResellPrice = ResellPrice(BigDecimal.valueOf(0), BigDecimal.valueOf(0))
}
