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

  lazy val transactionsAbstract = project("abstract")()

  lazy val transactions = project("core", transactionsAbstract)(
    "com.typesafe.slick" %% "slick" % "2.1.0"
  )

  lazy val transactionsSetup = project("setup", transactions)(
    "org.apache.tomcat"           %  "tomcat-jdbc"            % "7.0.39", //todo: upgrade!
    "com.github.tminglei"         %% "slick-pg_joda-time"     % "0.6.2",
    typesafeLogging
  )

  lazy val transactionsTesting = project("testing", transactions)()

  lazy val transactionsTestingH2 = project("testing-h2", transactionsTesting)(
    "org.scalatest" %% "scalatest"    % "2.1.7",
    "com.h2database" % "h2"           % "1.3.175"
  )

  lazy val transactionsTestingLiquibase = project("testing-liquibase", transactionsTestingH2)(
    typesafeLogging,
    "org.liquibase" % "liquibase-core" % "3.1.1"
  )

  val typesafeLogging = "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"

  lazy val root = Project(s"$basename-parent", file("."), settings = buildSettings)
    .aggregate(transactionsAbstract, transactions, transactionsSetup,
      transactionsTesting, transactionsTestingH2, transactionsTestingLiquibase)
}
