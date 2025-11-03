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

package sbt

import sbt.Keys._
import sbt.internal.inc._
import sbt.internal.inc.classfile.JavaAnalyze
import sbt.internal.inc.classpath.ClasspathUtil
import xsbti.{ AnalysisCallback, VirtualFile, VirtualFileRef }
import xsbti.compile.{ FileHash, MiniOptions, MiniSetup, SingleOutput }

object KotlinTest {
  private object EmptyLookup extends Lookup {
    override def changedClasspathHash: Option[Vector[FileHash]] = None

    override def analyses: Vector[xsbti.compile.CompileAnalysis] = Vector.empty

    override def lookupOnClasspath(binaryClassName: String): Option[VirtualFileRef] = None

    override def lookupAnalysis(binaryClassName: String): Option[xsbti.compile.CompileAnalysis] = None
    override def changedBinaries(previousAnalysis: xsbti.compile.CompileAnalysis): Option[Set[VirtualFileRef]] = None
    override def changedSources(previousAnalysis: xsbti.compile.CompileAnalysis): Option[xsbti.compile.Changes[VirtualFileRef]] = None
    override def removedProducts(previousAnalysis: xsbti.compile.CompileAnalysis): Option[Set[VirtualFileRef]] = None
    override def shouldDoIncrementalCompilation(changedClasses: Set[String], analysis: xsbti.compile.CompileAnalysis): Boolean = true

    override def hashClasspath(classpath: Array[VirtualFile]): java.util.Optional[Array[FileHash]] = java.util.Optional.empty()
  }

  val kotlinTests = Def.task {
    import sbt.internal.inc.PlainVirtualFileConverter.converter
    val defaultStampReader = Stamps.timeWrapBinaryStamps(converter)

    val out = ((Test / target).value ** "scala-*").get.head / "test-classes"
    val srcs = ((Test / sourceDirectory).value ** "*.kt").get.map(f => converter.toVirtualFile(f.toPath)).toList
    val xs = (out ** "*.class").get.map(_.toPath).toList

    val loader = ClasspathUtil.toLoader((Test / fullClasspath).value.map(_.data.toPath))
    val log = streams.value.log
    val output = new SingleOutput {
      def getOutputDirectory: File = out
    }
    val currentSetup = MiniSetup.of(
      output,
      MiniOptions.of(
        Array.empty,
        Array.empty,
        Array.empty
      ),
      "",
      CompileOrder.Mixed,
      false,
      Array.empty
    )
    val analysis = Incremental(
      srcs.toSet,
      converter,
      EmptyLookup,
      Analysis.Empty,
      incOptions.value,
      currentSetup,
      defaultStampReader,
      output,
      JarUtils.createOutputJarContent(output),
      None,
      None,
      None,
      log
    ) { (fs, changs, callback: AnalysisCallback, clsFileMgr) =>
      def readAPI(source: VirtualFileRef, classes: Seq[Class[?]]): Set[(String, String)] = {
        val (apis, mainClasses, inherits) = ClassToAPI.process(classes)
        apis.foreach(callback.api(source, _))
        mainClasses.foreach(callback.mainClass(source, _))
        inherits.map { case (from, to) =>
          (from.getName, to.getName)
        }
      }

      JavaAnalyze(xs, srcs, log, output, None)(callback, loader, readAPI)
    }._2
    val frameworks = (Test / loadedTestFrameworks).value.values.toList
    log.info(s"Compiling ${srcs.length} Kotlin source to $out...")
    Tests.discover(frameworks, analysis, log)._1
  }
}
