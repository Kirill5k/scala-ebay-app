package tasks.modules

import play.api.inject.{SimpleModule, bind}
import tasks.CexGenericStockMonitor

class CexGenericStockMonitorModule extends SimpleModule(bind[CexGenericStockMonitor].toSelf.eagerly())

