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

import sbt._

/**
 * @author
 *   pfnguyen
 */
object Keys {
  sealed trait KotlinCompileOrder

  val Kotlin = config("kotlin")
  val KotlinInternal = config("kotlin-internal").hide

  val kotlinCompile = TaskKey[Unit]("kotlin-compile", "runs kotlin compilation, occurs before normal compilation")
  val kotlincPluginOptions = TaskKey[Seq[String]]("kotlinc-plugin-options", "kotlin compiler plugin options")
  val kotlinSource = SettingKey[File]("kotlin-source", "kotlin source directory")
  val kotlinVersion = SettingKey[String]("kotlin-version", "version of kotlin to use for building")
  val kotlincOptions = SettingKey[Seq[String]]("kotlinc-options", "options to pass to the kotlin compiler")
  val kotlincJvmTarget = SettingKey[String]("kotlinc-jvm-target", "jvm target to use for building")

  def kotlinLib(name: String) = sbt.Keys.libraryDependencies +=
    "org.jetbrains.kotlin" % ("kotlin-" + name) % kotlinVersion.value

  def kotlinPlugin(name: String) = sbt.Keys.libraryDependencies +=
    "org.jetbrains.kotlin" % ("kotlin-" + name) % kotlinVersion.value % "compile-internal"

  def kotlinClasspath(config: Configuration, classpathKey: Def.Initialize[sbt.Keys.Classpath]): Setting[_] =
    config / kotlincOptions ++= {
      "-cp" :: classpathKey.value.map(_.data.getAbsolutePath).mkString(java.io.File.pathSeparator) ::
        Nil
    }

  case class KotlinPluginOptions(pluginId: String) {
    def option(key: String, value: String) =
      s"plugin:$pluginId:$key=$value"
  }
}
