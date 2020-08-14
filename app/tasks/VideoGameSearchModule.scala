package tasks

import play.api.inject.{SimpleModule, _}

class VideoGameSearchModule extends SimpleModule(bind[VideoGamesFinder].toSelf.eagerly())
