package domain

final case class PurchasableItem[D <: ItemDetails](
  itemDetails: D,
  purchasePrice: PurchasePrice
)

object PurchasableItem {
  type GenericPurchasableItem = PurchasableItem[ItemDetails.Generic]

  def generic(name: String, quantity: Int, price: BigDecimal): GenericPurchasableItem =
    PurchasableItem(
      ItemDetails.Generic(name),
      PurchasePrice(quantity, price)
    )
}
