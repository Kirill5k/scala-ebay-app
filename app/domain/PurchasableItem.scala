package domain

import domain.ItemDetails.GenericItemDetails

sealed trait PurchasableItem {
  def itemDetails: ItemDetails
  def purchasePrice: PurchasePrice
}

object PurchasableItem {
  final case class GenericPurchasableItem(
      itemDetails: GenericItemDetails,
      purchasePrice: PurchasePrice
  ) extends PurchasableItem

  def generic(name: String, quantity: Int, price: BigDecimal): PurchasableItem =
    GenericPurchasableItem(
      GenericItemDetails(name),
      PurchasePrice(quantity, price)
    )
}
