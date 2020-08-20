package services

import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.implicits._
import clients.cex.CexClient
import domain.ResellableItem.GenericItem
import domain.{ItemDetails, ResellableItem, SearchQuery, StockMonitorRequest, StockUpdate, StockUpdateType}
import javax.inject.Inject
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}

trait CexStockSearchService[F[_], D <: ItemDetails] {
  def getStockUpdates(request: StockMonitorRequest): F[List[StockUpdate[D]]]
}

final class GenericPurchasableItemService @Inject()(
    private val cexClient: CexClient
) extends CexStockSearchService[IO, ItemDetails.Generic] {

  private[services] val searchHistory: scala.collection.mutable.Set[SearchQuery] =
    scala.collection.mutable.Set[SearchQuery]()

  private[services] val cache: java.util.Map[String, ResellableItem[ItemDetails.Generic]] = ExpiringMap
    .builder()
    .expirationPolicy(ExpirationPolicy.ACCESSED)
    .expiration(25, TimeUnit.MINUTES)
    .build[String, ResellableItem[ItemDetails.Generic]]()

  override def getStockUpdates(request: StockMonitorRequest): IO[List[StockUpdate[ItemDetails.Generic]]] =
    cexClient
      .getCurrentStock[ItemDetails.Generic](request.query)
      .map(_.filter(_.itemDetails.fullName.isDefined))
      .flatMap { items =>
        if (!searchHistory.contains(request.query)) IO.pure(Nil) <* IO(updateCache(items))
        else IO(getStockUpdates(items, request.monitorStockChange, request.monitorPriceChange)) <* IO(updateCache(items))
      }
      .flatTap(_ => IO(searchHistory.add(request.query)))

  private def updateCache(items: List[ResellableItem[ItemDetails.Generic]]): Unit =
    items.foreach(i => cache.put(i.itemDetails.fullName.get, i))

  private def getStockUpdates(
      items: List[GenericItem],
      checkQuantity: Boolean,
      checkPrice: Boolean
  ): List[StockUpdate[ItemDetails.Generic]] =
    items.flatMap { i =>
      i.itemDetails.fullName.flatMap(n => Option(cache.get(n))) match {
        case None => Some(StockUpdate(StockUpdateType.New, i))
        case Some(prev) if checkQuantity && prev.price.quantityAvailable > i.price.quantityAvailable =>
          Some(StockUpdate(StockUpdateType.StockDecrease(prev.price.quantityAvailable, i.price.quantityAvailable), i))
        case Some(prev) if checkQuantity && prev.price.quantityAvailable < i.price.quantityAvailable =>
          Some(StockUpdate(StockUpdateType.StockIncrease(prev.price.quantityAvailable, i.price.quantityAvailable), i))
        case Some(prev) if checkPrice && prev.price.value > i.price.value =>
          Some(StockUpdate(StockUpdateType.PriceDrop(prev.price.value, i.price.value), i))
        case Some(prev) if checkPrice && prev.price.value < i.price.value =>
          Some(StockUpdate(StockUpdateType.PriceRaise(prev.price.value, i.price.value), i))
        case _ => None
      }
    }
}
