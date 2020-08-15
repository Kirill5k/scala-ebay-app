package services

import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.implicits._
import clients.cex.CexClient
import com.google.inject.Inject
import domain.{PurchasableItem, SearchQuery, StockUpdate, StockUpdateType}
import net.jodah.expiringmap.{ExpirationPolicy, ExpiringMap}

trait PurchasableItemService[F[_]] {

  def getStockUpdatesFromCex(query: SearchQuery): F[List[StockUpdate]]
}

final class GenericPurchasableItemService @Inject()(
    private val cexClient: CexClient
) extends PurchasableItemService[IO] {

  private[services] val searchHistory: scala.collection.mutable.Set[SearchQuery] =
    scala.collection.mutable.Set[SearchQuery]()

  private[services] val cache: java.util.Map[String, PurchasableItem] = ExpiringMap
    .builder()
    .expirationPolicy(ExpirationPolicy.ACCESSED)
    .expiration(1, TimeUnit.HOURS)
    .build[String, PurchasableItem]()

  override def getStockUpdatesFromCex(query: SearchQuery): IO[List[StockUpdate]] =
    cexClient.getCurrentStock(query)
      .map(_.filter(_.itemDetails.fullName.isDefined))
      .flatMap { items =>
        if (!searchHistory.contains(query)) IO.pure(Nil) <* IO(updateCache(items))
        else IO(getStockUpdates(items)) <* IO(updateCache(items))
      }
      .flatTap(_ => IO(searchHistory.add(query)))

  private def updateCache(items: List[PurchasableItem]): Unit =
    items.foreach(i => cache.put(i.itemDetails.fullName.get, i))

  private def getStockUpdates(items: List[PurchasableItem]): List[StockUpdate] = {
    items.flatMap { i =>
      i.itemDetails.fullName.flatMap(n => Option(cache.get(n))) match {
        case None => Some(StockUpdate(StockUpdateType.New, i))
        case Some(prev) if prev.purchasePrice.quantityAvailable > i.purchasePrice.quantityAvailable =>
          Some(StockUpdate(StockUpdateType.StockDecrease(prev.purchasePrice.quantityAvailable, i.purchasePrice.quantityAvailable), i))
        case Some(prev) if prev.purchasePrice.quantityAvailable < i.purchasePrice.quantityAvailable =>
          Some(StockUpdate(StockUpdateType.StockIncrease(prev.purchasePrice.quantityAvailable, i.purchasePrice.quantityAvailable), i))
        case Some(prev) if prev.purchasePrice.pricePerUnit > i.purchasePrice.pricePerUnit =>
          Some(StockUpdate(StockUpdateType.PriceDrop(prev.purchasePrice.pricePerUnit, i.purchasePrice.pricePerUnit), i))
        case Some(prev) if prev.purchasePrice.pricePerUnit < i.purchasePrice.pricePerUnit =>
          Some(StockUpdate(StockUpdateType.PriceIncrease(prev.purchasePrice.pricePerUnit, i.purchasePrice.pricePerUnit), i))
        case _ => None
      }
    }
  }
}
