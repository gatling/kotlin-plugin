/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
