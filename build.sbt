name := "kotlin-plugin"
organization := "io.gatling"
version := "2.0.1-SNAPSHOT"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

sbtPlugin := true
scalacOptions ++= Seq("-deprecation","-Xlint","-feature")
libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.3.8",
  "org.scalaz" %% "scalaz-core" % "7.3.7"
)

enablePlugins(BuildInfoPlugin, SbtPlugin)
buildInfoPackage := "kotlin"
scriptedLaunchOpts ++= Seq(
  "-Xmx1024m",
  "-Dplugin.org=" + organization.value,
  "-Dplugin.name=" + name.value,
  "-Dplugin.version=" + version.value)
