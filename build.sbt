import play.sbt.routes.RoutesKeys._
import Dependencies.Libraries._

name := """scala-ebay-app"""
organization := "io.kirill"
version := "1.0"

herokuAppName in Compile := "scala-ebay-app"
herokuJdkVersion in Compile := "13"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  guice,
  pureconfigCore,
  catsCore,
  catsEffect,
  fs2Core,
  sttpCore,
  sttpCats,
  sttpCirce,
  circeCore,
  circeGeneric,
  circeParser,
  circeExtras,
  reactiveMongoPlay,
  expiringMap,

  scalatestPlay % Test,
  mockitoCore % Test,
  mockitoScalatest % Test,
  embeddedMongo % Test
)

routesImport ++= Seq("common.binders.QueryStringBinders._", "java.time.Instant")
