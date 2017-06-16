name := "kamon-cloudwatch"

name := "0.1"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).settings(
  fork in Test := true
)

val akkaVersion = "2.4.18"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion

libraryDependencies += "io.kamon" % "kamon-core_2.11" % "0.6.7"

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.133"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"