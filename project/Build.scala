import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "googleopenconnect"
  val appVersion      = "1.0"

  val appDependencies = Seq(
    "com.google.api-client" % "google-api-client" % "1.19.0",
    "com.google.apis" % "google-api-services-oauth2" % "v2-rev87-1.19.0",
    "com.google.http-client" % "google-http-client-jackson2" % "1.19.0"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    organization := "com.lunatech"
  )

}
