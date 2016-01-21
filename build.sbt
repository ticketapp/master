import playscalajs.PlayScalaJS.autoImport._
import com.typesafe.sbt.gzip.Import._
import com.typesafe.sbt.web.Import._
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.Play.autoImport._
import play.sbt.PlayScala
import sbt.Keys._
import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.cross.CrossType
import playscalajs.ScalaJSPlay
import sbt.Project.projectToRef
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

name := "Claude"

version := "0.001"

lazy val clients = Seq(client)
lazy val scalaV = "2.11.7"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd, gzip),
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    cache,
    evolutions,
    ws,
    filters,
    "org.webjars" % "jquery" % "1.11.1",
    "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
    "org.webjars" % "angularjs" % "1.4.8",
    "net.codingwell" %% "scala-guice" % "4.0.0",
    "org.webjars" %% "webjars-play" % "2.4.0-1",
    "org.postgresql" % "postgresql" % "9.4-1205-jdbc42",
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
    specs2 % Test,
    "com.github.docker-java" % "docker-java" % "1.4.0",
    "com.zaxxer" % "HikariCP" % "2.4.1",
  //  "com.google.guava" % "guava" % "18.0",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.0" % "test"
  )
).enablePlugins(PlayScala).
    aggregate(clients.map(projectToRef): _*).
    dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    "com.lihaoyi" %%% "upickle" % "0.3.4",
    "com.greencatsoft" %%% "scalajs-angular" % "0.6"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSPlay).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
  )
  .jsSettings()
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

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

cancelable in Global := true