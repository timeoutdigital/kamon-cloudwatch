name := "kamon-cloudwatch"

name := "0.1"

organization := "com.timeout"

releaseCrossBuild := true

scalaVersion := "2.11.8"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

crossScalaVersions := Seq("2.11.8", "2.12.2")

credentials += Credentials(Path.userHome / ".bintray" / ".credentials")

lazy val root = (project in file(".")).settings(
  fork in Test := true
)

val akkaVersion = "2.4.19"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion

libraryDependencies += "io.kamon" % "kamon-core_2.11" % "0.6.7"

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.133"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"