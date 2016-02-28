import com.typesafe.sbt.gzip.Import._
import com.typesafe.sbt.web.Import._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.cross.CrossType
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.Play.autoImport._
import play.sbt.PlayScala
import playscalajs.PlayScalaJS.autoImport._
import playscalajs.ScalaJSPlay
import sbt.Keys._
import sbt.Project.projectToRef
import sbt._

name := "Claude"

maintainer:= "Simon Garnier"

dockerExposedPorts in Docker := Seq(88, 9443)

version := "0.001"

lazy val clients = Seq(client)
lazy val scalaV = "2.11.7"
lazy val akkaVersion = "2.4.0"
lazy val libraryVersion = "1.2.0"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "spray repo" at "http://repo.spray.io"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd, gzip),
  libraryDependencies ++= Seq(
  cache,
  evolutions,
  ws,
  filters,
  specs2 % Test,
  "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
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
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "com.lihaoyi" %% "sourcecode" % "0.1.0",
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  )
).enablePlugins(PlayScala)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  scalaJSUseRhino in Test := false,
  scalaJSStage in Test := FastOptStage,
  testFrameworks := Seq(new TestFramework("com.greencatsoft.greenlight.Greenlight")),
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    "com.lihaoyi" %%% "upickle" % "0.3.8",
    "com.greencatsoft" %%% "scalajs-angular" % "0.6",
    "com.greencatsoft" %%% "greenlight" % "0.3" % "test"
  ),
  jsDependencies += RuntimeDOM,
  jsDependencies += "org.webjars" % "angularjs" % "1.3.8" / "angular.min.js" % "test",
  jsDependencies += ("org.webjars" % "angularjs" % "1.3.8" / "angular-mocks.js" dependsOn "angular.min.js") % "test"
).enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
  )
  .jsSettings()
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

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
