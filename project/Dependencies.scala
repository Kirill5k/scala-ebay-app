import sbt._

object Dependencies {

  object Versions {
    lazy val pureconfig        = "0.12.3"
    lazy val circe             = "0.13.0"
    lazy val mockito           = "1.10.3"
    lazy val reactiveMongoPlay = "0.20.3-play28"
    lazy val sttp              = "2.1.1"
  }

  object Libraries {
    lazy val pureconfigCore = "com.github.pureconfig" %% "pureconfig"  % Versions.pureconfig
    lazy val catsCore       = "org.typelevel"         %% "cats-core"   % "2.1.0"
    lazy val catsEffect     = "org.typelevel"         %% "cats-effect" % "2.1.1"
    lazy val fs2Core        = "co.fs2"                %% "fs2-core"    % "2.2.2"

    lazy val sttpCore  = "com.softwaremill.sttp.client" %% "core"                           % Versions.sttp
    lazy val sttpCirce = "com.softwaremill.sttp.client" %% "circe"                          % Versions.sttp
    lazy val sttpCats  = "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % Versions.sttp

    lazy val circeCore         = "io.circe"          %% "circe-core"           % Versions.circe
    lazy val circeGeneric      = "io.circe"          %% "circe-generic"        % Versions.circe
    lazy val circeParser       = "io.circe"          %% "circe-parser"         % Versions.circe
    lazy val circeExtras       = "io.circe"          %% "circe-generic-extras" % Versions.circe
    lazy val reactiveMongoPlay = "org.reactivemongo" %% "play2-reactivemongo"  % Versions.reactiveMongoPlay

    lazy val expiringMap = "net.jodah" % "expiringmap" % "0.5.9"

    lazy val scalatestPlay    = "org.scalatestplus.play" %% "scalatestplus-play"       % "5.0.0"
    lazy val mockitoCore      = "org.mockito"            %% "mockito-scala"            % Versions.mockito
    lazy val mockitoScalatest = "org.mockito"            %% "mockito-scala-scalatest"  % Versions.mockito
    lazy val embeddedMongo    = "de.flapdoodle.embed"    % "de.flapdoodle.embed.mongo" % "2.2.0"
  }
}
