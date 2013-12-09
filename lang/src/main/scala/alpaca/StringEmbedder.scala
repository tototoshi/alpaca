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

import scala.util.parsing.combinator.RegexParsers

object StringEmbedder {
  val embeddedVariableRegex = """[$][{]?([^}]+)[}]?""".r

  class StringEvaluationException(message: String) extends Exception

  object StringParser extends RegexParsers with EmbeddedVariableParser {
    override def skipWhitespace = false

    def escapedDaller = "\\$" ^^ {
      _ => "$"
    }

    def plain: Parser[(Symbol, String)] = not("$") ~> (escapedDaller | """.""".r) ^^ {
      case s => ('plain, s.mkString)
    }

    def embeddedVar: Parser[(Symbol, String)] = super.embeddedVariable ^^ {
      case embeddedVariableRegex(e) => ('embedded, e)
    }

    def str: Parser[List[(Symbol, String)]] = rep(plain | embeddedVar)

    def parse(s: String): List[(Symbol, String)] = parseAll(str, s).getOrElse(
      throw new StringEvaluationException("Error while evaluating: " + s)
    )
  }

  def getEmbeddedVariable(s: String): List[Symbol] = {
    StringParser.parse(s).collect { case ('embedded, s) => Symbol(s) }
  }

  def embed(s: String, values: Map[Symbol, String]): String = {
    StringParser.parse(s).map {
      case (tpe, str) =>
        if (tpe == 'embedded) {
          values.getOrElse(Symbol(str), throw new UndefinedFunctionException(Symbol(str)))
        } else {
          str
        }
    }.mkString
  }
}
