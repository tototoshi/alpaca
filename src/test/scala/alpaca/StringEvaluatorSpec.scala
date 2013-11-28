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

class StringEvaluatorSpec extends FunSpec with ShouldMatchers with OptionValues {

  it("should parse string and get embedded variable name.") {
    StringEmbedder.getEmbeddedVariable("a${b}c${HOME}def") should be(List('b, 'HOME))
    StringEmbedder.getEmbeddedVariable("a$HOME") should be(List('HOME))
  }

  it("should replace ${...} with environment variable.") {
    StringEmbedder.embed("abc${HOME}def", Map('HOME -> sys.env("HOME"))) should be("abc" + sys.env("HOME") + "def")
  }

}
