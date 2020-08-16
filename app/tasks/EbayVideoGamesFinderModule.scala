package tasks

import play.api.inject._

class EbayVideoGamesFinderModule extends SimpleModule(bind[EbayVideoGamesFinder].toSelf.eagerly())
