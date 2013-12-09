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

sealed trait AST

object AST {

  abstract sealed class Factor extends AST

  case class StringFactor(value: String) extends Factor

  case class ListFactor(value: List[AST]) extends Factor

  case class PlusOps(left: AST, right: AST) extends AST

  trait Selector

  case class Name(selector: AST) extends Selector

  case class Id(selector: AST) extends Selector

  case class ClassName(selector: AST) extends Selector

  case class LinkText(selector: AST) extends Selector

  case class PartialLinkText(selector: AST) extends Selector

  case class Css(selector: AST) extends Selector

  case class XPath(selector: AST) extends Selector

  case class GoTo(url: AST) extends AST

  case class Fill(selector: Selector, text: AST) extends AST

  case class Click(selector: Selector) extends AST

  case class Select(text: AST, selector: Selector) extends AST

  case class FunctionDef(name: Symbol, args: List[Symbol], statements: List[AST]) extends AST

  case class FunctionCall(name: Symbol, args: List[AST]) extends AST

  case object Close extends AST

  case object Submit extends AST

  case class Require(filename: StringFactor) extends AST

  case class Assignment(name: Symbol, value: AST) extends AST

  case class Reassignment(name: Symbol, value: AST) extends AST

  case class Variable(name: Symbol) extends AST

  case class ListAccess(name: Symbol, index: Int) extends AST

  case class If(cond: AST, ifStatements: List[AST], elseStatements: List[AST]) extends AST

  case class EqualOps(left: AST, right: AST) extends AST

  case class LessOps(left: AST, right: AST) extends AST

  case class Foreach(array: AST, name: Symbol, statements: List[AST]) extends AST

  case class Println(value: AST) extends AST

  case class ShellExec(command: List[String]) extends AST
}
