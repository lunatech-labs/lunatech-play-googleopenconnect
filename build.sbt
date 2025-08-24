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
      "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
    )
  )
)

name := "play-googleopenconnect"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

crossScalaVersions := Seq("2.13.16", "3.7.2")

libraryDependencies ++= Seq(
  ws, guice,
  "com.google.api-client" % "google-api-client" % "2.8.1",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev20200213-2.0.0"
)

ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / dynverVTagPrefix := false

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
