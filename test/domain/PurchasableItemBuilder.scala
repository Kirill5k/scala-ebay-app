package domain

object PurchasableItemBuilder {

  def generic(name: String, quantity: Int = 1, price: Double = 1800.0): PurchasableItem[ItemDetails.Generic] =
    PurchasableItem.generic(name, quantity, BigDecimal(price))
}
