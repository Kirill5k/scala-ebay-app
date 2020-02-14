
name := """scala-ebay-app"""
organization := "io.kirill"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

lazy val circeVersion = "0.12.3"
lazy val mockitoVersion = "1.10.3"

libraryDependencies ++= Seq(
  ehcache,
  guice,
  ws,

  "org.typelevel" %% "cats-core" % "2.1.0",
  "org.typelevel" %% "cats-effect" % "2.1.1",
  "co.fs2" %% "fs2-core" % "2.2.2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.20.2-play28",

  "net.jodah" % "expiringmap" % "0.5.9",

  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "org.mockito" %% "mockito-scala" % mockitoVersion % Test,
  "org.mockito" %% "mockito-scala-scalatest" % mockitoVersion % Test,
  "com.github.simplyscala" % "scalatest-embedmongo_2.12" % "0.2.4" % Test,
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.2.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.kirill.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.kirill.binders._"
