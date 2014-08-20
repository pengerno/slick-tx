organization := "no.penger"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

scalacOptions := Seq("-deprecation")

name := "slick-transactions"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick"     % "2.1.0",
  "com.h2database"      %  "h2"       % "1.3.175"  % "test",
  "org.scalatest"      %% "scalatest" % "2.1.7"    % "test"
)
