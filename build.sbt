name := "play-googleopenconnect"

organization := "com.lunatech"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

crossScalaVersions := Seq("2.13.11", "3.3.0")

libraryDependencies ++= Seq(
  ws, guice,
  "com.google.api-client" % "google-api-client" % "1.30.7",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev20190313-1.30.1"
)

ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / dynverVTagPrefix := false

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
