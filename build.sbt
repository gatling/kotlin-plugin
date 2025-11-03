import _root_.io.gatling.build.license.GatlingLicenseFileKeys.gatlingLicenseFileTask

Global / githubPath := "gatling/kotlin-plugin"
Global / gatlingDevelopers := List(
  GatlingDeveloper("slandelle@gatling.io", "Stephane Landelle", isGatlingCorp = true),
  GatlingDeveloper("gcorre@gatling.io", "Guillaume Corré", isGatlingCorp = true),
  GatlingDeveloper("ggaly@gatling.io", "Guillaume Galy", isGatlingCorp = true)
)

name := "kotlin-plugin"

licenses := Seq("MIT" -> url("https://opensource.org/license/MIT"))
headerLicense := Some(sbtheader.License.MIT("2015", "Perry"))

Compile / gatlingLicenseFileTask := {
  val file = (Compile / resourceManaged).value / "META-INF" / "LICENSE"
  val license = baseDirectory.value / "LICENSE"
  IO.transfer(license, file)
  Seq(file)
}

sbtPlugin := true
crossSbtVersions := Seq("1.9.0") // Used for checking compatibility with sbt 1.4+

scalacOptions ++= Seq("-deprecation", "-Xlint", "-feature")
libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.3.8"
)

enablePlugins(SbtPlugin, GatlingOssPlugin)
scriptedLaunchOpts ++= Seq(
  "-Xmx1024m",
  "-Dplugin.org=" + organization.value,
  "-Dplugin.name=" + name.value,
  "-Dplugin.version=" + version.value
)
