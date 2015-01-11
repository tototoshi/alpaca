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
import com.typesafe.scalalogging._

trait BetweenParser extends RegexParsers with LazyLogging {

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

  def string: Parser[StringFactor] = intString | stringLiteral | embeddedVariableFactor

  def array: Parser[ListFactor] = between("[", repsep(expression, ","), "]") ^^ {
    case xs => ListFactor(xs)
  }

  def url: Parser[AST] = string

  def selector(prefix: Parser[String]): Parser[StringFactor] = prefix ~> string

  def nameSelector: Parser[Selector] = selector("n") ^^ Name

  def linkTextSelector: Parser[Selector] = selector("t") ^^ LinkText

  def partialLinkTextSelector: Parser[Selector] = selector("pt") ^^ PartialLinkText

  def xpathSelector = selector("x") ^^ XPath

  def stringSelector: Parser[Selector] = expression ^^ Css

  def selector: Parser[Selector] = nameSelector | linkTextSelector | partialLinkTextSelector | xpathSelector | stringSelector

  def factor: Parser[AST] = string | array | arrayAccess | variable

  def expression: Parser[AST] = binaryOps | ifElseExp | shellExec | functionCall | factor

  def binaryOps: Parser[AST] = (ifElseExp | shellExec | functionCall | factor) ~ ("+" | "==" | "!=" | "<=" | ">=" | "<" | ">") ~ expression ^^ {
    case x ~ "+" ~ y => PlusOps(x, y)
    case x ~ "==" ~ y => EqualOps(x, y)
    case x ~ "!=" ~ y => NotEqualOps(x, y)
    case x ~ "<" ~ y => LessOps(x, y)
    case x ~ ">" ~ y => GreaterOps(x, y)
    case x ~ "<=" ~ y => LessThanOps(x, y)
    case x ~ ">=" ~ y => GreaterThanOps(x, y)
  }

  def fillWith: Parser[AST] = "fill" ~> parentheses(selector ~ "," ~ expression) ^^ {
    case s ~ _ ~ sl => Fill(s, sl)
  }

  def select: Parser[AST] = "select" ~> parentheses(selector ~ "," ~ string) ^^ {
    case sel ~ _ ~ str => Select(str, sel)
  }

  def click: Parser[Click] = "click" ~> parentheses(selector) ^^ Click

  def statement: Parser[AST] =
    require | assignment | reassignment | foreach | functionDef | printLn | fillWith | click | select

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

  def printLn: Parser[AST] = "print" ~> (expression | parentheses(expression)) ^^ Println

  def ifElseExp: Parser[AST] =
    ("if" ~>
      parentheses(expression) ~
      between("{", script, "}") ~
      opt("else" ~>
        (between("{", script, "}") | (ifElseExp ^^ { List(_) }))
      )) ^^ {
        case cond ~ ifStatements ~ elseStatements => If(cond, ifStatements, elseStatements.getOrElse(Nil))
      }

  def foreach: Parser[AST] = "foreach" ~> parentheses(expression ~ "as" ~ variableName) ~ between("{", script, "}") ^^ {
    case a ~ _ ~ b ~ c => {
      Foreach(a, b, c)
    }
  }

  def shellCommand: Parser[List[String]] = rep(
    doubleQuotedString | """([^) ])+""".r // TODO
  )

  def shellExec: Parser[AST] = "$(" ~> shellCommand <~ ")" ^^ ShellExec

  def parse(in: String): List[AST] = parseAll(script, in) match {
    case Success(result, _) => {
      result.foreach(r => logger.debug(r.toString))
      result
    }
    case Failure(error, rest) => {
      throw new SyntaxError(error)
    }
    case Error(error, _) => throw new UnexpectedException(error)
  }

}
