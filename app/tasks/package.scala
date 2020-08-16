import play.api.inject.{SimpleModule, bind}

package object tasks {
  class CexGenericStockMonitorModule extends SimpleModule(bind[CexGenericStockMonitor].toSelf.eagerly())
  class EbayVideoGamesFinderModule extends SimpleModule(bind[EbayVideoGamesFinder].toSelf.eagerly())

}
