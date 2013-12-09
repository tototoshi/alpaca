package alpaca

object EmbeddedFunctions {

  def allAsMap: Map[Symbol, List[Value] => Value] = Map(
    'len -> len
  )

  def len(args: List[Value]): Value = Value.intValue(args.size)

}
