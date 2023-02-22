package demo

import java.util.Optional
import kotlin.jvm.optionals.*
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.ExperimentalTime

// Test some Kotlin 1.8 features
@ExperimentalTime
fun main() {
  // Optional extension methods now stable
  val presentOptional = Optional.of("I'm here!")
  println(presentOptional.getOrNull())

  val absentOptional = Optional.empty<String>()
  println(absentOptional.getOrNull())
  println(absentOptional.getOrDefault("Nobody here!"))
  println(absentOptional.getOrElse {
    println("Optional was absent!")
    "Default value!"
  })

  // comparable and subtractable TimeMarks (experimental)
  val timeSource = TimeSource.Monotonic
  val mark1 = timeSource.markNow()
  Thread.sleep(500) // Sleep 0.5 seconds
  val mark2 = timeSource.markNow()
  println(mark2 > mark1) // TimeMarks are now comparable
}
