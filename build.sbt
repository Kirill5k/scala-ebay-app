import play.sbt.routes.RoutesKeys._

name := """scala-ebay-app"""
organization := "io.kirill"
version := "1.0"

herokuAppName in Compile := "scala-ebay-app"
herokuJdkVersion in Compile := "13"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(guice) ++ Dependencies.core ++ Dependencies.test

routesImport ++= Seq("common.binders.QueryStringBinders._", "java.time.Instant")
