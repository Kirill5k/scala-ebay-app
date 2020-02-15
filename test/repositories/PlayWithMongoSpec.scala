package repositories

import de.flapdoodle.embed.mongo.{MongodExecutable, MongodStarter}
import de.flapdoodle.embed.mongo.config.{IMongodConfig, MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import play.modules.reactivemongo.ReactiveMongoApi
import tasks.VideoGameSearchModule

trait PlayWithMongoSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterAll {

  var mongodExecutable: MongodExecutable = null

  override def beforeAll(): Unit = {
    val starter: MongodStarter = MongodStarter.getDefaultInstance
    val mongodConfig: IMongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", 12345, Network.localhostIsIPv6()))
      .build()

    mongodExecutable = starter.prepare(mongodConfig)
    val mongod = mongodExecutable.start
  }

  override def afterAll(): Unit = {
    mongodExecutable.stop()
  }

  override def fakeApplication = new GuiceApplicationBuilder()
    .disable[VideoGameSearchModule]
    .configure("mongodb.uri" -> "mongodb://localhost:12345/mongo-test")
    .build()

  lazy val reactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]
}