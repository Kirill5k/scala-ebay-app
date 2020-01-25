package repositories

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import play.modules.reactivemongo.ReactiveMongoApi

trait PlayWithMongoSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting {

  override def fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> "mongodb://localhost:27017/mongo-test")
    .build()

  lazy val reactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]
}