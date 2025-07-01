Global / githubPath := "gatling/kotlin-plugin"
Global / gatlingDevelopers := List(
  GatlingDeveloper("slandelle@gatling.io", "Stephane Landelle", isGatlingCorp = true),
  GatlingDeveloper("gcorre@gatling.io", "Guillaume Corré", isGatlingCorp = true),
  GatlingDeveloper("ggaly@gatling.io", "Guillaume Galy", isGatlingCorp = true)
)

name := "kotlin-plugin"

sbtPlugin := true
crossSbtVersions := Seq("1.8.2", "1.4.9") // Used for checking compatibility with sbt 1.4+

scalacOptions ++= Seq("-deprecation", "-Xlint", "-feature")
libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.3.7"
)

enablePlugins(SbtPlugin, GatlingOssPlugin)
scriptedLaunchOpts ++= Seq(
  "-Xmx1024m",
  "-Dplugin.org=" + organization.value,
  "-Dplugin.name=" + name.value,
  "-Dplugin.version=" + version.value
)
