package kotlin

import Keys.*
import sbt.{Keys as _, *}
import sbt.Keys.*
import sbt.plugins.JvmPlugin

/**
 * @author pfnguyen
 */
object KotlinPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override def projectConfigurations = KotlinInternal :: Nil

  private def kotlinScriptCompilerDeps(kotlinVer: String) = {
    import scala.math.Ordering.Implicits.infixOrderingOps

    if (KotlinVersion(kotlinVer) <= KotlinVersion("1.3.21")) {
      Seq(
        "org.jetbrains.kotlin" % "kotlin-script-runtime" % kotlinVer
      )
    } else {
      Seq(
        "org.jetbrains.kotlin" % "kotlin-scripting-compiler-embeddable" % kotlinVer % KotlinInternal.name,
        "org.jetbrains.kotlin" % "kotlin-scripting-compiler-embeddable" % kotlinVer
      )
    }
  }

  override def projectSettings = Seq(
    libraryDependencies ++= Seq(
      "org.jetbrains.kotlin" % "kotlin-compiler-embeddable" % kotlinVersion.value % KotlinInternal.name
    ) ++ kotlinScriptCompilerDeps(kotlinVersion.value),
    KotlinInternal / managedClasspath := Classpaths.managedJars(KotlinInternal, classpathTypes.value, update.value),
    kotlinVersion := "1.3.50",
    kotlincJvmTarget := "1.8",
    kotlincOptions := Nil,
    kotlincPluginOptions := Nil,
    watchSources     ++= {
      import language.postfixOps
      val kotlinSources = "*.kt" || "*.kts"
      (Compile / sourceDirectories).value.flatMap(_ ** kotlinSources get) ++
        (Test / sourceDirectories).value.flatMap(_ ** kotlinSources get)
    }
  ) ++ inConfig(Compile)(kotlinCompileSettings) ++
    inConfig(Test)(kotlinCompileSettings)

  val autoImport = Keys

  // public to allow kotlin compile in other configs beyond Compile and Test
  val kotlinCompileSettings = List(
    unmanagedSourceDirectories += kotlinSource.value,
    kotlincOptions := kotlincOptions.value,
    kotlincJvmTarget := kotlincJvmTarget.value,
    kotlincPluginOptions := kotlincPluginOptions.value,
    kotlinCompile := Def.task {
        KotlinCompile.compile(kotlincOptions.value,
          kotlincJvmTarget.value,
          kotlinVersion.value,
          sourceDirectories.value, kotlincPluginOptions.value,
          dependencyClasspath.value, (KotlinInternal / managedClasspath).value,
          classDirectory.value, streams.value)
    }.dependsOn (Compile / compile / compileInputs).value,
    compile := (compile dependsOn kotlinCompile).value,
    kotlinSource := sourceDirectory.value / "kotlin",
    Test / definedTests ++= KotlinTest.kotlinTests.value
  )
}
