package demo
fun main(args: Array<String>) {
  // Test some Kotlin 1.4 features
  val isEven = IntPredicate { it % 2 == 0 }
  println("Is 7 even? - ${isEven.accept(7)}")
}
// SAM conversions for Kotlin interfaces with the 'fun' modifier
fun interface IntPredicate {
  fun accept(i: Int): Boolean
}


