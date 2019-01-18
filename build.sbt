name := "play-googleopenconnect"

organization := "com.lunatech"

version := "2.3.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.11.8", "2.12.8")

libraryDependencies ++= Seq(
  ws, guice,
  "com.google.api-client" % "google-api-client" % "1.28.0",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev20180628-1.28.0"
)

publishTo in ThisBuild := version { v: String =>
  val path = if (v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
  Some(Resolver.url("Lunatech Artifactory", new URL("http://artifactory.lunatech.com/artifactory/%s/" format path)))
}.value
