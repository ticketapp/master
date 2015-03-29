name := "ticketapp"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "ws.securesocial" %% "securesocial" % "2.1.4",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.scalatestplus" % "play_2.10" % "1.0.0" % "test"
)

play.Project.playScalaSettings
