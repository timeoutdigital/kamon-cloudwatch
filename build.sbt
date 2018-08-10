name := "kamon-cloudwatch"

organization := "com.timeout"

releaseCrossBuild := true

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

//credentials += Credentials(Path.userHome / ".bintray" / ".credentials")

//bintrayOrganization := Some("timeoutdigital")

//bintrayRepository := "releases"

libraryDependencies ++= Seq(
  "io.kamon"      %% "kamon-core"              % "1.1.3",
  "com.amazonaws" %  "aws-java-sdk-cloudwatch" % "1.11.384",
  "org.scalatest" %% "scalatest"               % "3.0.5" % "test"
)

fork in Test := true
