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

import java.io.File
import java.lang.reflect.{ Field, Method }
import java.util.jar.JarEntry

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.math.Ordered.orderingToOrdered
import scala.util.Try

import sbt._
import sbt.Keys.{ Classpath, TaskStreams }
import sbt.internal.inc.classpath.ClasspathUtil
import sbt.io._

/**
 * @author
 *   pfnguyen
 */
object KotlinCompile {

  def grepjar(jarfile: File)(pred: JarEntry => Boolean): Boolean =
    jarfile.isFile && Using.jarFile(false)(jarfile) { in =>
      in.entries.asScala exists pred
    }

  lazy val kotlinMemo = scalaz.Memo.immutableHashMapMemo[Classpath, KotlinReflection](cp => KotlinReflection.fromClasspath(cp))

  def compile(
      options: Seq[String],
      jvmTarget: String,
      kotlinVersion: String,
      sourceDirs: Seq[File],
      kotlinPluginOptions: Seq[String],
      classpath: Classpath,
      compilerClasspath: Classpath,
      output: File,
      s: TaskStreams
  ): Unit = {
    import language.reflectiveCalls
    val stub = KotlinStub(s, kotlinMemo(compilerClasspath))
    val args = stub.compilerArgs
    stub.parse(kotlinVersion, args.instance, options.toList)
    val kotlinFiles = "*.kt" || "*.kts"
    val javaFiles = "*.java"

    val kotlinSources = sourceDirs.flatMap(d => (d ** kotlinFiles).get).distinct
    val javaSources =
      sourceDirs.filterNot(f => sourceDirs.exists(f0 => f0.relativeTo(f).isDefined && f != f0)) map (d => (d, (d ** javaFiles).get)) filter (_._2.nonEmpty)
    if (kotlinSources.isEmpty) {
      s.log.debug("No sources found, skipping kotlin compile")
    } else {
      s.log.debug(s"Compiling sources $kotlinSources")
      def pluralizeSource(count: Int) =
        if (count == 1) "source" else "sources"
      val message =
        s"Compiling ${kotlinSources.size} Kotlin ${pluralizeSource(kotlinSources.size)}"
      s.log.info(message)
      args.freeArgs = (kotlinSources ++ javaSources.map(_._1)).map(_.getAbsolutePath).asJava
      args.noStdlib = true
      args.jvmTarget = jvmTarget
      val fcpjars = classpath.map(_.data.getAbsoluteFile)
      val (pluginjars, cpjars) = fcpjars.partition {
        grepjar(_)(_.getName.startsWith("META-INF/services/org.jetbrains.kotlin.compiler.plugin"))
      }
      val cp = cpjars.mkString(File.pathSeparator)
      val pcp = pluginjars.map(_.getAbsolutePath).toArray
      args.classpath = Option(args.classpath[String]).fold(cp)(_ + File.pathSeparator + cp)
      args.pluginClasspaths = Option(args.pluginClasspaths[Array[String]]).fold(pcp)(_ ++ pcp)
      args.pluginOptions = Option(args.pluginOptions[Array[String]]).fold(kotlinPluginOptions.toArray)(_ ++ kotlinPluginOptions.toArray[String])
      output.mkdirs()
      args.destination = output.getAbsolutePath
      stub.compile(args.instance)
    }
  }
}

object KotlinReflection {
  def fromClasspath(cp: Classpath): KotlinReflection = {
    val cl = ClasspathUtil.toLoader(cp.map(_.data.toPath))
    val compilerClass = cl.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    val servicesClass = cl.loadClass("org.jetbrains.kotlin.config.Services")
    val messageCollectorClass = cl.loadClass("org.jetbrains.kotlin.cli.common.messages.MessageCollector")
    val commonCompilerArgsClass = cl.loadClass("org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments")

    val compilerExec = Try(
      compilerClass.getMethod("exec", messageCollectorClass, servicesClass, commonCompilerArgsClass)
    ).toOption.getOrElse {

      val commonToolArguments = cl.loadClass("org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments")
      val clitool = cl.loadClass("org.jetbrains.kotlin.cli.common.CLITool")
      clitool.getMethod("exec", messageCollectorClass, servicesClass, commonToolArguments)
    }

    KotlinReflection(
      cl,
      servicesClass,
      compilerClass,
      cl.loadClass("org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments"),
      messageCollectorClass,
      commonCompilerArgsClass,
      compilerExec,
      servicesClass.getDeclaredField("EMPTY")
    )
  }
}
case class KotlinReflection(
    cl: ClassLoader,
    servicesClass: Class[_],
    compilerClass: Class[_],
    compilerArgsClass: Class[_],
    messageCollectorClass: Class[_],
    commonCompilerArgsClass: Class[_],
    compilerExec: Method,
    servicesEmptyField: Field
)
case class KotlinStub(s: TaskStreams, kref: KotlinReflection) {
  import language.reflectiveCalls

  import kref.*

  def messageCollector: AnyRef = {
    type CompilerMessageLocation = {
      def getPath: String
      def getLine: Int
      def getColumn: Int
    }

    import java.lang.reflect.{ InvocationHandler, Proxy }
    val messageCollectorInvocationHandler = new InvocationHandler {
      val _messages = new ArrayBuffer[String]
      override def invoke(proxy: AnyRef, method: Method, args: Array[AnyRef]): AnyRef =
        if (method.getName == "hasErrors") {
          // this is now required for some reason
          Predef.boolean2Boolean(_messages.nonEmpty)
        } else if (method.getName == "report") {
          val Array(severity, message, location) = args
          val l = location.asInstanceOf[CompilerMessageLocation]
          val msg = Option(l).map(x => x.getPath).fold(message.toString)(loc => loc + ": " + l.getLine + ", " + l.getColumn + ": " + message)
          severity.toString match {
            case "INFO"           => s.log.info(msg)
            case "WARNING"        => s.log.warn(msg)
            case "STRONG_WARNING" => s.log.warn(msg)
            case "ERROR" | "EXCEPTION" =>
              s.log.error(msg)
              _messages += msg
            case "OUTPUT" | "LOGGING" => s.log.debug(msg)
          }
          null
        } else {
          null
        }
    }

    Proxy.newProxyInstance(cl, Array(messageCollectorClass), messageCollectorInvocationHandler)
  }

  def parse(kotlinVersion: String, args: Object, options: List[String]): Unit = {
    // TODO FIXME, this is much worse than it used to be, the parsing api has been
    // deeply in flux since 1.1.x
    val parser = kref.cl.loadClass("org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt")
    val commonToolArguments = cl.loadClass(
      "org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments"
    )
    import collection.JavaConverters.*
    if (KotlinVersion(kotlinVersion) < KotlinVersion("1.7.0")) {
      val parserMethod = parser.getMethod(
        "parseCommandLineArguments",
        classOf[java.util.List[java.lang.String]],
        commonToolArguments
      )
      parserMethod.invoke(null, options.asJava, args)
    } else {
      val parserMethod = parser.getMethod(
        "parseCommandLineArguments",
        classOf[java.util.List[java.lang.String]],
        commonToolArguments,
        classOf[Boolean]
      )
      parserMethod.invoke(null, options.asJava, args, false: java.lang.Boolean)
    }
  }

  def compilerArgs = {
    import language.dynamics
    new Dynamic {
      def withFirstUpper(string: String): String = string.head.toUpper + string.tail
      def getterName(field: String) = s"get${withFirstUpper(field)}"
      def setterName(field: String) = s"set${withFirstUpper(field)}"

      def selectDynamic[A](field: String): A = {
        val methodName = getterName(field)
        val getterOpt = compilerArgsClass.getMethods.find(_.getName == methodName)
        getterOpt match {
          case Some(getter) => getter.invoke(instance).asInstanceOf[A]
          case None         => compilerArgsClass.getField(field).get(instance).asInstanceOf[A]
        }
      }

      def updateDynamic(field: String)(value: Any): Unit = {
        val methodName = setterName(field)
        val setterOpt = compilerArgsClass.getMethods.find(_.getName == methodName)
        setterOpt match {
          case Some(setter) => setter.invoke(instance, value.asInstanceOf[Object])
          case None         => compilerArgsClass.getField(field).set(instance, value)
        }
      }

      val instance = compilerArgsClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    }
  }

  def compile(args: AnyRef): Unit = {
    val compiler = compilerClass.getDeclaredConstructor().newInstance()
    val result = compilerExec.invoke(compiler, messageCollector, servicesEmptyField.get(null), args: java.lang.Object)
    result.toString match {
      case "COMPILATION_ERROR" | "INTERNAL_ERROR" =>
        throw new MessageOnlyException("Compilation failed. See log for more details")
      case _ =>
    }
  }
}
