package domain

sealed trait StockUpdateType

object StockUpdateType {
  final case object New extends StockUpdateType
  final case object SoldOut extends StockUpdateType
  final case class PriceDrop(previous: BigDecimal, current: BigDecimal) extends StockUpdateType
  final case class PriceIncrease(previous: BigDecimal, current: BigDecimal) extends StockUpdateType
  final case class StockIncrease(previous: Int, current: Int) extends StockUpdateType
  final case class StockDecrease(previous: Int, current: Int) extends StockUpdateType
}


final case class StockUpdate(
    updateType: StockUpdateType,
    purchasableItem: PurchasableItem
)
