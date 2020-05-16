package controllers

import domain.ResellableItem

sealed trait ResellableItemResponse

object ResellableItemResponse {
  final case class ItemSummary(name: Option[String], url: String, price: BigDecimal)
  final case class ItemsSummary(total: Int, items: Seq[ItemSummary])
  final case class ResellableItemsSummaryResponse(total: Int, unrecognized: ItemsSummary, profitable: ItemsSummary, rest: ItemsSummary) extends ResellableItemResponse

  def resellableItemsSummaryResponse(items: Seq[ResellableItem]): ResellableItemResponse = {
    val withoutResellPrice = items.filter(_.resellPrice.isEmpty)
    val profitableForResell = items.filter(i => i.resellPrice.exists(rp => rp.cash > i.listingDetails.price))
    val rest = items.filter(i => !withoutResellPrice.contains(i) && !profitableForResell.contains(i))
    ResellableItemsSummaryResponse(
      items.size,
      toItemsSummary(withoutResellPrice),
      toItemsSummary(profitableForResell),
      toItemsSummary(rest)
    )
  }

  private def toItemsSummary(items: Seq[ResellableItem]): ItemsSummary =
    ItemsSummary(items.size, items.map(i => ItemSummary(i.itemDetails.summary, i.listingDetails.url, i.listingDetails.price)))
}
