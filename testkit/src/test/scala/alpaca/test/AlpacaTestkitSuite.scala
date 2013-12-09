package alpaca.test

import org.scalatest.{ ShouldMatchers, FunSuite }

class AlpacaTestkitSuite extends FunSuite with ShouldMatchers {

  val script =
    """|--TEST--
      |test name
      |--FILE--
      |test script
      |--EXPECT--
      |expect
    """.stripMargin

  test("extract test name") {
    AlpacaTestkit.extractTestName(script.lines) should be("test name")
  }

  test("extract script") {
    AlpacaTestkit.extractScript(script.lines) should be("test script")
  }

  test("extract expected output") {
    AlpacaTestkit.extractExpectedOutput(script.lines) should be("expect")
  }

}
