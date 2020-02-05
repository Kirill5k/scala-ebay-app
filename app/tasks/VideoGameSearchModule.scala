package tasks

import play.api.inject.{SimpleModule, _}

class VideoGameSearchModule extends SimpleModule(bind[VideoGameSearchTask].toSelf.eagerly())
