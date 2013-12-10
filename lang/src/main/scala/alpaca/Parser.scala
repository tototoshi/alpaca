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
import AST._
import com.typesafe.scalalogging.slf4j.Logging

trait BetweenParser extends RegexParsers with Logging {

  def between[A, B, C](p1: Parser[A], p2: Parser[B], p3: Parser[C]): Parser[B] = p1 ~> p2 <~ p3

  def between[A, B](p1: Parser[A], p2: Parser[B]): Parser[B] = p1 ~> p2 <~ p1

}

trait EmbeddedVariableParser extends RegexParsers with BetweenParser {

  def embeddedVariableNaked: Parser[String] = "$" ~> """[A-Za-z_]+""".r ^^ {
    name => "${" + name + "}"
  }

  def embeddedVariableBraced: Parser[String] = "$" ~ between("{", """[A-Za-z_]+""".r, "}") ^^ { case x ~ y => x + y }

  def embeddedVariable: Parser[String] = embeddedVariableNaked | embeddedVariableBraced
}

object Parser extends RegexParsers with BetweenParser with EmbeddedVariableParser {

  def parentheses[A](p: Parser[A]): Parser[A] = between("(", p, ")")

  def embeddedVariableFactor: Parser[StringFactor] = super.embeddedVariable ^^ StringFactor

  def intLiteral: Parser[Int] = """[0-9]+""".r ^^ { _.toInt }

  def intString: Parser[StringFactor] = intLiteral ^^ { case i => StringFactor(i.toString) }

  def doubleQuotedString: Parser[String] = """"([^"\p{Cntrl}\\]|\\[\\'"bfnrt$]|\\u[a-fA-F0-9]{4})*"""".r ^^ {
    case s => s.drop(1).dropRight(1)
  }

  def stringLiteral: Parser[StringFactor] = doubleQuotedString ^^ StringFactor

  def stringAtom: Parser[StringFactor] = intString | stringLiteral | embeddedVariableFactor

  def string: Parser[StringFactor] = stringAtom

  def array: Parser[ListFactor] = between("[", repsep(expression, ","), "]") ^^ {
    case xs => ListFactor(xs)
  }

  def url: Parser[AST] = string

  def selector(prefix: Parser[String]): Parser[StringFactor] = prefix ~> parentheses(string)

  def nameSelector: Parser[Selector] = selector("name") ^^ Name

  def idSelector: Parser[Selector] = selector("id") ^^ Id

  def classNameSelector: Parser[Selector] = selector("class") ^^ ClassName

  def linkTextSelector: Parser[Selector] = selector("link") ^^ LinkText

  def partialLinkTextSelector: Parser[Selector] = selector("partialLink") ^^ PartialLinkText

  def xpathSelector = selector("xpath") ^^ XPath

  def cssSelector: Parser[Selector] = selector("css") ^^ Css

  def stringSelector: Parser[Selector] = expression ^^ Css

  def selector: Parser[Selector] =
    nameSelector |
      idSelector |
      classNameSelector |
      linkTextSelector |
      partialLinkTextSelector |
      cssSelector |
      xpathSelector |
      stringSelector

  def factor: Parser[AST] = string | array | arrayAccess | variable

  def expression: Parser[AST] = ifElseExp | op | shellExec | functionCall | factor

  def plusOps: Parser[AST] = factor ~ "+" ~ expression ^^ {
    case x ~ _ ~ y => PlusOps(x, y)
  }

  def op: Parser[AST] = plusOps

  def fillWith: Parser[AST] = "fill" ~> parentheses(selector ~ "," ~ expression) ^^ {
    case s ~ _ ~ sl => Fill(s, sl)
  }

  def select: Parser[AST] = "select" ~> parentheses(selector ~ "," ~ string) ^^ {
    case sel ~ _ ~ str => Select(str, sel)
  }

  def click: Parser[Click] = "click" ~> parentheses(selector) ^^ Click

  def statement: Parser[AST] = {
    (require | assignment | reassignment | foreach | functionDef | printLn | fillWith | click | select) ^^ {
      case e => {
        logger.debug("parsed: " + e)
        e
      }
    }
  }

  def script: Parser[List[AST]] = rep(statement | expression)

  def functionSignature: Parser[Symbol] = """[A-Za-z0-9_]+""".r ^^ Symbol.apply

  def functionArgDefs: Parser[List[Symbol]] = parentheses(repsep(variableName, ","))

  def functionDef: Parser[FunctionDef] = "def" ~> functionSignature ~ opt(functionArgDefs) ~ between("{", script, "}") ^^ {
    case sig ~ args ~ sc => FunctionDef(sig, args.getOrElse(Nil), sc)
  }

  def functionArgs: Parser[List[AST]] = parentheses(repsep(expression, ","))

  def functionCall: Parser[FunctionCall] = functionSignature ~ functionArgs ^^ {
    case name ~ args => FunctionCall(name, args)
  }

  def require: Parser[AST] = "require" ~> stringLiteral ^^ Require

  def variableName: Parser[Symbol] = """\w+""".r ^^ Symbol.apply

  def variable: Parser[AST] = variableName ^^ Variable

  def arrayAccess: Parser[ListAccess] = variableName ~ between("[", intLiteral, "]") ^^ {
    case v ~ i => ListAccess(v, i)
  }

  def assignment: Parser[AST] = "var" ~> variableName ~ "=" ~ expression ^^ {
    case n ~ _ ~ e => Assignment(n, e)
  }

  def reassignment: Parser[AST] = variableName ~ "=" ~ expression ^^ {
    case n ~ _ ~ e => Reassignment(n, e)
  }

  def printLn: Parser[AST] = "print" ~> expression ^^ Println

  def equalOps: Parser[AST] = expression ~ "==" ~ expression ^^ {
    case leftExpression ~ _ ~ rightExpression => EqualOps(leftExpression, rightExpression)
  }

  def lessOps: Parser[AST] = expression ~ "<" ~ expression ^^ {
    case leftExpression ~ _ ~ rightExpression => LessOps(leftExpression, rightExpression)
  }

  def compareOps: Parser[AST] = equalOps | lessOps

  def ifElseExp: Parser[AST] = "if" ~> parentheses(compareOps) ~ between("{", script, "}") ~ opt("else" ~> between("{", script, "}")) ^^ {
    case cond ~ ifStatements ~ elseStatements => If(cond, ifStatements, elseStatements.getOrElse(Nil))
  }

  def foreach: Parser[AST] = "foreach" ~> parentheses((array | variable) ~ "as" ~ variableName) ~ between("{", script, "}") ^^ {
    case a ~ _ ~ b ~ c => {
      Foreach(a, b, c)
    }
  }

  def shellCommand: Parser[List[String]] = rep(
    doubleQuotedString | """([^) ])+""".r // TODO
  )

  def shellExec: Parser[AST] = "$(" ~> shellCommand <~ ")" ^^ ShellExec

  def parse(in: String): List[AST] = parseAll(script, in) match {
    case Success(result, _) => result
    case Failure(error, rest) => {
      throw new SyntaxError(error)
    }
    case Error(error, _) => throw new UnexpectedException(error)
  }

}
