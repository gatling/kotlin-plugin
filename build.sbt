name := "kotlin-plugin"
organization := "io.gatling"
version := "2.0.1-SNAPSHOT"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
homepage := Some(url("https://gatling.io"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/gatling/kotlin-plugin"),
    "scm:git:https://github.com/gatling/kotlin-plugin.git",
    "scm:git:git@github.com/gatling/kotlin-plugin.git"
  )
)
developers := List(
  Developer("slandelle@gatling.io", "Stephane Landelle", "slandelle@gatling.io", url("https://gatling.io")),
  Developer("ggaly@gatling.io", "Guillaume Galy", "ggaly@gatling.io", url("https://gatling.io")),
)

sbtPlugin := true
crossSbtVersions := Seq("1.8.2", "1.4.9") // Used for checking compatibility with sbt 1.4+

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

// Publishing
publishMavenStyle := true
sonatypeProfileName := "io.gatling"
sonatypeCredentialHost := "s01.oss.sonatype.org"
