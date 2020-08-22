import play.sbt.routes.RoutesKeys._

name := """scala-ebay-app"""
organization := "io.kirill"
version := "1.0"

Compile / herokuAppName := "scala-ebay-app"
Compile / herokuJdkVersion := "13"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(guice) ++ Dependencies.core ++ Dependencies.test

routesImport ++= Seq("common.binders.QueryStringBinders._", "java.time.Instant")
