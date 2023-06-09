name := "play-googleopenconnect"

organization := "com.lunatech"

version := "2.9.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.10"

crossScalaVersions := Seq("2.12.17", "2.13.10")

libraryDependencies ++= Seq(
  ws, guice,
  "com.google.api-client" % "google-api-client" % "1.30.7",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev20190313-1.30.1"
)

ThisBuild / versionScheme := Some("semver-spec")

Global / onChangedBuildSource := ReloadOnSourceChanges

githubOwner := "lunatech-labs"
githubRepository := "lunatech-play-googleopenconnect"
githubTokenSource := TokenSource.Or(
  TokenSource.Environment("GITHUB_TOKEN"), // Injected during a github workflow for publishing
  TokenSource.Environment("SHELL"),  // safe to assume this will be set in all our devs environments, usually /bin/bash, doesn't matter what it is to prevent local errors
)
