package tasks

import play.api.inject._

class CexGenericStockMonitorModule extends SimpleModule(bind[CexGenericStockMonitor].toSelf.eagerly())
