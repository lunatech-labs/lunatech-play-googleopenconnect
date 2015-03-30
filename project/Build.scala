import sbt._
import sbt.Keys._

object ApplicationBuild extends Build {

  val appName = "play-googleopenconnect"
  val appVersion = "1.1"

  val appDependencies = Seq(
    "com.google.api-client" % "google-api-client" % "1.19.0",
    "com.google.apis" % "google-api-services-oauth2" % "v2-rev87-1.19.0",
    "com.google.http-client" % "google-http-client-jackson2" % "1.19.0"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    organization := "com.lunatech",


    publishTo in ThisBuild <<= version { (v: String) =>
      val path = if (v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
      Some(Resolver.url("Lunatech Artifactory", new URL("http://artifactory.lunatech.com/artifactory/%s/" format path)))
    }
  )
}
