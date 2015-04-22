import aether.Aether._
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin._

object Build extends sbt.Build {

  val basename = "tx"

  lazy val buildSettings = Defaults.coreDefaultSettings ++
    aetherSettings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++
    releaseSettings ++
    Seq(
      scalacOptions            := Seq("-unchecked", "-deprecation", "-encoding", "UTF-8", "-feature"),
      organization             := "no.penger",
      scalaVersion             := "2.10.4",
      crossScalaVersions       := Seq("2.10.4", "2.11.4"),
      ReleaseKeys.crossBuild   := true,
      publishMavenStyle        := true,
      publish                 <<= deploy,
      publishTo               <<= version { v =>
        val proxy = "http://mavenproxy.finntech.no/finntech-internal-"
        val end = if(v endsWith "SNAPSHOT") "snapshot" else "release"
        Some("Finn-" + end at proxy + end)
      }
    )

  def project(suffix: String, deps: ModuleID*) =
    Project(
      id           = s"$basename-$suffix",
      base         = file(s"./$suffix"),
      settings     = buildSettings ++ Seq(libraryDependencies ++= deps)
    )

  object deps{
    val slick      = "com.typesafe.slick"  %% "slick"              % "2.1.0"
    val tomcatJdbc = "org.apache.tomcat"    % "tomcat-jdbc"        % "7.0.53"
    val logging    = "org.slf4j"            % "slf4j-api"          % "1.7.7"

    val pgJodaTime = "com.github.tminglei" %% "slick-pg_joda-time" % "0.6.5.3"
    val postgres   = "org.postgresql"       % "postgresql"         % "9.3-1102-jdbc41"
    val scalatest  = "org.scalatest"       %% "scalatest"          % "2.2.2"
    val h2         = "com.h2database"       % "h2"                 % "1.4.187"
    val liquibase  = "org.liquibase"        % "liquibase-core"     % "3.1.1"
  }

  lazy val txAbstract = project("abstract")
  lazy val txCore     = project("core", deps.slick) dependsOn txAbstract
  lazy val txSetup    = project("setup", deps.tomcatJdbc, deps.pgJodaTime, deps.postgres) dependsOn txCore
  lazy val txTest     = project("testing") dependsOn txCore
  lazy val txTestH2   = project("testing-h2", deps.scalatest, deps.h2) dependsOn txTest
  lazy val txTestH2L  = project("testing-liquibase", deps.logging, deps.liquibase) dependsOn txTestH2

  lazy val root = Project(s"$basename-parent", file("."), settings = buildSettings)
    .aggregate(txAbstract, txCore, txSetup, txTest, txTestH2, txTestH2L)
}
