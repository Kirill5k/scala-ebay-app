
name := """scala-ebay-app"""
organization := "io.kirill"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  ehcache,
  guice,
  ws,
  "org.typelevel" %% "cats-core" % "2.1.0",

  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "org.mockito" % "mockito-all" % "1.8.4" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.kirill.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.kirill.binders._"
