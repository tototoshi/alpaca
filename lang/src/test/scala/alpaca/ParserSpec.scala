/*
 * Copyright 2013 Toshiyuki Takahashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpaca

import org.scalatest.{ OptionValues, ShouldMatchers, FunSpec }
import alpaca.AST._

class ParserSpec extends FunSpec with ShouldMatchers with OptionValues {

  it("should parse string literal") {
    val result = Parser.parse(Parser.stringLiteral, """"abc"""")
    result.get should be(StringFactor("abc"))
  }

  it("should parse 'visit'") {
    val result = Parser.parse("visit(\"http://github.com\")")
    result should be(List(GoTo(StringFactor("http://github.com"))))
  }

  it("should parse 'fill'") {
    val result = Parser.parse("""fill(name("email"), "foo@example.com")""")
    result should be(List(Fill(Name(StringFactor("email")), StringFactor("foo@example.com"))))
  }

  it("should parse 'click'") {
    val result = Parser.parse("""click("Scala")""")
    result should be(List(Click(Css(StringFactor("Scala")))))
  }

  it("should parse shellcomamnd") {
    val result = Parser.parse("""$(echo "a b c" | cut -f -d " ")""")
    result should be(List(ShellExec(List("echo", "a b c", "|", "cut", "-f", "-d", " "))))
  }

  it("should parse stringLiteral") {
    val result = Parser.parse("""" """")
    result should be(List(StringFactor(" ")))
  }

}
