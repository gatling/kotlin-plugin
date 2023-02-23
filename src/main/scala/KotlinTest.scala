package sbt

import sbt.Keys.*
import sbt.internal.inc.{Analysis, ClassToAPI, Incremental, JarUtils, Lookup, Stamps}
import sbt.internal.inc.classfile.JavaAnalyze
import sbt.internal.inc.classpath.ClasspathUtil
import xsbti.compile.{CompileAnalysis, FileHash, MiniOptions, MiniSetup, SingleOutput}
import xsbti.{AnalysisCallback, VirtualFile, VirtualFileRef}

object KotlinTest {
  private object EmptyLookup extends Lookup {
    override def changedClasspathHash: Option[Vector[FileHash]] = None

    override def analyses: Vector[CompileAnalysis] = Vector.empty

    override def lookupOnClasspath(binaryClassName: String): Option[VirtualFileRef] = None

    override def lookupAnalysis(binaryClassName: String): Option[CompileAnalysis] = None
    override def changedBinaries(previousAnalysis: xsbti.compile.CompileAnalysis): Option[Set[VirtualFileRef]] = None
    override def changedSources(previousAnalysis: xsbti.compile.CompileAnalysis): Option[xsbti.compile.Changes[VirtualFileRef]] = None
    override def removedProducts(previousAnalysis: xsbti.compile.CompileAnalysis): Option[Set[VirtualFileRef]] = None
    override def shouldDoIncrementalCompilation(changedClasses: Set[String],analysis: xsbti.compile.CompileAnalysis): Boolean = true

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
    )(
      (fs, changs, callback: AnalysisCallback, clsFileMgr) => {
        def readAPI(source: VirtualFileRef, classes: Seq[Class[?]]): Set[(String, String)] = {
          val (apis, mainClasses, inherits) = ClassToAPI.process(classes)
          apis.foreach(callback.api(source, _))
          mainClasses.foreach(callback.mainClass(source, _))
          inherits.map {
            case (from, to) => (from.getName, to.getName)
          }
        }

        JavaAnalyze(xs, srcs, log, output, None)(callback, loader, readAPI)
      }
    )._2
    val frameworks = (Test / loadedTestFrameworks).value.values.toList
    log.info(s"Compiling ${srcs.length} Kotlin source to $out...")
    Tests.discover(frameworks, analysis, log)._1
  }
}
