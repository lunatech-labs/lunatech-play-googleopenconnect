name := "play-googleopenconnect"

organization := "com.lunatech"

version := "1.3-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  ws,
  "com.google.api-client" % "google-api-client" % "1.22.0",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev124-1.22.0"
)

publishTo in ThisBuild <<= version { (v: String) =>
  val path = if (v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
  Some(Resolver.url("Lunatech Artifactory", new URL("http://artifactory.lunatech.com/artifactory/%s/" format path)))
}