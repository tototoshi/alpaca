package alpaca

object EmbeddedFunctions {

  def allAsMap: Map[Symbol, List[Value] => Environment => Value] = Map(
    'len -> len,
    'sleep -> sleep,
    'exit -> exit,
    'accept -> accept
  )

  def len(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('len, 1, args.size)
    }
    val arg = args(0)
    Value.intValue(arg.get.asInstanceOf[List[_]].size)
  }

  def sleep(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('sleep, 1, args.size)
    }
    Thread.sleep(Value.asInt(args(0)))
    Value.nullValue
  }

  def exit(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('sleep, 1, args.size)
    }
    sys.exit(Value.asInt(args(0)))
  }

  def accept(args: List[Value])(environment: Environment): Value = {
    environment.driver.switchTo().alert().accept()
    Value.nullValue
  }
}
