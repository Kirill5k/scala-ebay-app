package repositories

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoApi

trait PlayWithMongoSpec extends PlaySpec with GuiceOneAppPerSuite {

  override def fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> "mongodb://localhost:27017/todos-test")
    .build()

  implicit val reactiveMongoApi: ReactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]
}