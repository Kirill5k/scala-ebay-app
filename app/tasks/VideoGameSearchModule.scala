package tasks

import play.api.inject._

class VideoGameSearchModule extends SimpleModule(bind[VideoGamesFinder].toSelf.eagerly())
