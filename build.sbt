name := "play-googleopenconnect"

organization := "com.lunatech"

version := "2.5.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.11.8", "2.12.8")

libraryDependencies ++= Seq(
  ws, guice,
  "com.google.api-client" % "google-api-client" % "1.29.0",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev150-1.25.0"
)

publishTo in ThisBuild := version { v: String =>
  val path = if (v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
  Some(Resolver.url("Lunatech Artifactory", new URL("https://artifactory.lunatech.com/artifactory/%s/" format path)))
}.value
