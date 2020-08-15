package tasks

import play.api.inject._

class GenericPurchasableItemSearchModule extends SimpleModule(bind[GenericPurchasableItemFinder].toSelf.eagerly())
