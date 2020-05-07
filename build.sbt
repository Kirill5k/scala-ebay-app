import play.sbt.routes.RoutesKeys._

name := """scala-ebay-app"""
organization := "io.kirill"
version := "1.0"

herokuAppName in Compile := "scala-ebay-app"
herokuJdkVersion in Compile := "13"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

lazy val circeVersion = "0.13.0"
lazy val mockitoVersion = "1.10.3"
lazy val reactiveMongoPlayVersion = "0.20.3-play28"
lazy val sttpVersion = "2.1.1"

libraryDependencies ++= Seq(
  guice,

  "org.typelevel" %% "cats-core" % "2.1.0",
  "org.typelevel" %% "cats-effect" % "2.1.1",
  "co.fs2" %% "fs2-core" % "2.2.2",

  "com.softwaremill.sttp.client" %% "core" % sttpVersion,
  "com.softwaremill.sttp.client" %% "circe" % sttpVersion,
  "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % sttpVersion,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoPlayVersion,

  "net.jodah" % "expiringmap" % "0.5.9",

  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "org.mockito" %% "mockito-scala" % mockitoVersion % Test,
  "org.mockito" %% "mockito-scala-scalatest" % mockitoVersion % Test,
  "com.github.simplyscala" % "scalatest-embedmongo_2.12" % "0.2.4" % Test,
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.2.0" % Test
)

routesImport ++= Seq("binders.QueryStringBinders._", "java.time.Instant")
