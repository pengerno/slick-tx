import aether.Aether._
import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  val basename = "tx"

  override def settings = super.settings ++ Seq(
    scalacOptions      := Seq("-unchecked", "-deprecation", "-encoding", "UTF-8", "-feature"),
    organization       := "no.penger",
    scalaVersion       := "2.11.2"
  )

  lazy val buildSettings = Defaults.coreDefaultSettings ++ aetherSettings ++ Seq(
    crossScalaVersions := Seq("2.10.4", "2.11.2"),
    publishMavenStyle  := true,
    publish <<= deploy,
    publishTo <<= version { v =>
      val proxy = "http://mavenproxy.finntech.no/finntech-internal-"
      val end = if(v endsWith "SNAPSHOT") "snapshot" else "release"
      Some("Finn-" + end at proxy + end)
    }
  )

  def project(suffix: String, projectDeps: sbt.ClasspathDep[sbt.ProjectReference]*)(deps: ModuleID*) =
    Project(
      id           = s"$basename-$suffix",
      base         = file(s"./$suffix"),
      dependencies = projectDeps,
      settings     = buildSettings ++ Seq(libraryDependencies ++= deps)
    )

  object deps{
    val slick      = "com.typesafe.slick"  %% "slick"              % "2.1.0"
    val tomcatJdbc = "org.apache.tomcat"    % "tomcat-jdbc"        % "7.0.39"  //todo: upgrade!
    val logging    = "org.slf4j"            % "slf4j-api"          % "1.7.7"

    val pgJodaTime = "com.github.tminglei" %% "slick-pg_joda-time" % "0.6.2"
    val scalatest  = "org.scalatest"       %% "scalatest"          % "2.1.7"
    val h2         = "com.h2database"       % "h2"                 % "1.3.175"
    val liquibase  = "org.liquibase"        % "liquibase-core"     % "3.1.1"
  }

  lazy val txAbstract = project("abstract"    )()
  lazy val txCore     = project("core",       txAbstract)(deps.slick)
  lazy val txSetup    = project("setup",      txCore)(deps.tomcatJdbc, deps.pgJodaTime, deps.logging)
  lazy val txTest     = project("testing",    txCore)()
  lazy val txTestH2   = project("testing-h2", txTest)(deps.scalatest, deps.h2)
  lazy val txTestH2L  = project("testing-liquibase", txTestH2)(deps.logging, deps.liquibase)

  lazy val root = Project(s"$basename-parent", file("."), settings = buildSettings)
    .aggregate(txAbstract, txCore, txSetup, txTest, txTestH2, txTestH2L)
}
