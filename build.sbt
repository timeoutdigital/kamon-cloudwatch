name := "kamon-cloudwatch"

version := "0.0.2"

organization := "com.timeout"

releaseCrossBuild := true

scalaVersion := "2.11.11"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

crossScalaVersions := Seq(scalaVersion.value, "2.12.3")

credentials += Credentials(Path.userHome / ".bintray" / ".credentials")

bintrayOrganization := Some("timeoutdigital")

bintrayRepository := "releases"

lazy val root = (project in file(".")).settings(
  fork in Test := true
)

libraryDependencies += "io.kamon" %% "kamon-core" % "1.0.0-RC1"

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.151"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"
