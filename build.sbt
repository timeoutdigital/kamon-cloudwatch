name := "kamon-cloudwatch"

organization := "com.timeout"

releaseCrossBuild := true

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

credentials += Credentials(Path.userHome / ".bintray" / ".credentials")

bintrayOrganization := Some("timeoutdigital")

bintrayRepository := "releases"

lazy val root = (project in file(".")).settings(
  fork in Test := true
)

libraryDependencies += "io.kamon" %% "kamon-core" % "1.0.0-RC1"

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.151"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"
