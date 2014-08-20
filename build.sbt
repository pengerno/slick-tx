organization := "no.penger"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

name := "slick-transactions-parent"

lazy val transactions = project.in(file("transactions")).settings(
  name                 := "slick-transactions",
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "2.1.0"
  )
)

lazy val transactionsTest = project.in(file("transactions-test")).dependsOn(transactions).settings(
  name                 := "slick-transactions-test",
  libraryDependencies ++= Seq(
    "org.scalatest"  %% "scalatest"      % "2.1.7",
    "com.h2database"  % "h2"             % "1.3.175"
  )
)

lazy val transactionsTestLiquibase = project.in(file("transactions-test-liquibase")).dependsOn(transactionsTest).settings(
  name                 := "slick-transactions-test-liquibase",
  libraryDependencies ++= Seq(
    "org.liquibase"   % "liquibase-core" % "3.1.1"
  )
)

