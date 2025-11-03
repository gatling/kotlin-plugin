/*
 * Copyright (c) 2015 Perry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kotlin

import sbt.{ Keys => _, _ }
import sbt.Keys._
import sbt.plugins.JvmPlugin

/**
 * @author
 *   pfnguyen
 */
object KotlinPlugin extends AutoPlugin {

  import Keys._

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
    watchSources ++= {
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
    kotlinCompile := Def
      .task {
        KotlinCompile.compile(
          kotlincOptions.value,
          kotlincJvmTarget.value,
          kotlinVersion.value,
          sourceDirectories.value,
          kotlincPluginOptions.value,
          dependencyClasspath.value,
          (KotlinInternal / managedClasspath).value,
          classDirectory.value,
          streams.value
        )
      }
      .dependsOn(Compile / compile / compileInputs)
      .value,
    compile := (compile dependsOn kotlinCompile).value,
    kotlinSource := sourceDirectory.value / "kotlin",
    Test / definedTests ++= KotlinTest.kotlinTests.value
  )
}
