import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.PlayScala

import scalariform.formatter.preferences._
import sbt._
import Keys._
import play.sbt.Play.autoImport._
import PlayKeys._

name := "Claude"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  cache,
  evolutions,
  ws,
  filters,
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
  "com.mohiva" %% "play-silhouette" % "3.0.2",
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.2" % "test",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "com.github.tminglei" %% "slick-pg" % "0.9.1",
  "com.vividsolutions" % "jts" % "1.13",
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.2" % "test",
  specs2 % Test,
  "com.github.docker-java" % "docker-java" % "1.4.0",
  "me.lessis" %% "undelay" % "0.1.0",
  "me.lessis" %% "odelay-core" % "0.1.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)
