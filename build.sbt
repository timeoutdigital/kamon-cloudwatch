name := "kamon-cloudwatch"

version := "0.0.2-SNAPSHOT"

organization := "com.timeout"

releaseCrossBuild := true

scalaVersion := "2.11.11"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

crossScalaVersions := Seq("2.11.11", "2.12.2")

credentials += Credentials(Path.userHome / ".bintray" / ".credentials")

bintrayOrganization := Some("timeoutdigital")

bintrayRepository := "releases"

lazy val root = (project in file(".")).settings(
  fork in Test := true
)

val akkaVersion = "2.4.19"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion

libraryDependencies += "io.kamon" %% "kamon-core" % "0.6.7" exclude ("com.typesafe.akka", "akka-actor_2.11")

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.151"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"