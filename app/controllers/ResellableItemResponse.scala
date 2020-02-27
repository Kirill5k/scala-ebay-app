package controllers

import domain.ResellableItem

sealed trait ResellableItemResponse

object ResellableItemResponse {
  final case class ResellableItemsSummary(total: Int, withoutResellPrice: Int, profitableForReselling: Int) extends ResellableItemResponse

  def itemsSummary(items: Seq[ResellableItem]): ResellableItemResponse = {
    val withoutResellPrice = items.count(_.resellPrice.isEmpty)
    val profitableForResell = items.count(i => i.resellPrice.exists(rp => rp.cash > i.listingDetails.price))
    ResellableItemsSummary(items.size, withoutResellPrice, profitableForResell)
  }
}
