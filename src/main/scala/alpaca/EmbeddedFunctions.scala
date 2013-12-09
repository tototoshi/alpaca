package alpaca

object EmbeddedFunctions {

  def allAsMap: Map[Symbol, List[Value] => Value] = Map(
    'len -> len,
    'sleep -> sleep
  )

  def len(args: List[Value]): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('len, 1, args.size)
    }
    val arg = args(0)
    Value.intValue(arg.get.asInstanceOf[List[_]].size)
  }

  def sleep(args: List[Value]): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('sleep, 1, args.size)
    }
    Thread.sleep(args(0).get.toString.toInt)
    Value.nullValue
  }
}
