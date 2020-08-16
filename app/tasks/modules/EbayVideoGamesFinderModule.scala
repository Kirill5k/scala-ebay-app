package tasks.modules

import play.api.inject.{SimpleModule, bind}
import tasks.EbayVideoGamesFinder

class EbayVideoGamesFinderModule extends SimpleModule(bind[EbayVideoGamesFinder].toSelf.eagerly())

