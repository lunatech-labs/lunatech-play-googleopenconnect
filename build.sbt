name := "play-googleopenconnect"

organization := "com.lunatech"

version := "2.8.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

crossScalaVersions := Seq("2.12.15", "2.13.8")

libraryDependencies ++= Seq(
  ws, guice,
  "com.google.api-client" % "google-api-client" % "1.34.1",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev20190313-1.30.1"
)

ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / publishTo := version { v: String =>
  val path = if (v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
  Some(Resolver.url("Lunatech Artifactory", new URL("https://artifactory.lunatech.com/artifactory/%s/" format path)))
}.value
