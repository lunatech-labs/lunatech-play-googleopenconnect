
inThisBuild(
  List(
    organization := "com.lunatech",
    organizationName := "Lunatech",
    organizationHomepage := Some(url("https://lunatech.com")),
    homepage := Some(url("https://github.com/lunatech-labs/lunatech-play-googleopenconnect/")),
    developers := List(
      Developer(
        "wjglerum",
        "Willem Jan Glerum",
        "willem.jan.glerum@lunatech.nl",
        url("https://github.com/wjglerum")
      )
    ),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/license/LICENSE-2.0")
    )
  )
)

name := "play-googleopenconnect"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

crossScalaVersions := Seq("2.13.11", "3.3.1")

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
