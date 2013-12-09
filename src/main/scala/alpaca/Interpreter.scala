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

import AST._
import com.typesafe.scalalogging.slf4j.Logging
import org.openqa.selenium.{ StaleElementReferenceException, By }
import java.io.File
import scala.collection.mutable.{ Set => MutableSet, ListBuffer }

object Interpreter extends Logging {

  private def by(selector: Selector)(implicit environment: Environment): By = selector match {
    case Name(s) => By.name(evaluate(s).get.toString)
    case Id(s) => By.id(evaluate(s).get.toString)
    case ClassName(s) => By.className(evaluate(s).get.toString)
    case Css(s) => By.cssSelector(evaluate(s).get.toString)
    case LinkText(s) => By.linkText(evaluate(s).get.toString)
    case PartialLinkText(s) => By.partialLinkText(evaluate(s).get.toString)
    case XPath(s) => By.xpath(evaluate(s).get.toString)
  }

  type FilePath = String
  var fileAlreadyLoaded: MutableSet[FilePath] = MutableSet.empty

  def loadFile(file: File): List[AST] = {
    val path = file.getCanonicalPath
    if (!fileAlreadyLoaded.contains(path)) {
      fileAlreadyLoaded.add(path)
      Parser.parse(io.Source.fromFile(file).getLines().filterNot(_.startsWith("#")).mkString("\n"))
    } else {
      Nil
    }
  }

  def evaluateStatements(exprs: List[AST])(implicit environment: Environment): Value = {
    var retval: Value = Value.nullValue
    for (expr <- exprs) {
      retval = evaluate(expr)
    }
    retval
  }

  def lookupEmbeddedFunctions(name: Symbol)(implicit env: Environment): Option[List[Value] => Value] =
    EmbeddedFunctions.allAsMap.get(name)

  def evaluate(expr: AST)(implicit environment: Environment): Value = {
    expr match {
      case StringFactor(s) => {
        val embeddedValues =
          StringEmbedder.getEmbeddedVariable(s)
            .map { ev => ev -> environment.getVariable(ev).map(_.get.toString).getOrElse(sys.env(ev.name)) }
            .toMap
        Value(StringType, StringEmbedder.embed(s, embeddedValues))
      }
      case ListFactor(xs) => {
        Value(ListType, xs.map { case x => evaluate(x).get })
      }
      case PlusOps(left, right) => {
        val l = evaluate(left)
        val r = evaluate(right)
        if (l.tpe == StringType) {
          Value(l.tpe, l.get.toString + r.get.toString)
        } else {
          throw new UnsupportedOperationException("'+' is not supported")
        }
      }
      case GoTo(url) => {
        environment.driver.get(evaluate(url).get.toString)
        environment.clearCurrentTargetElement()
        logger.info(s"go to $url")
        Value.nullValue
      }
      case Fill(selector, text) => {
        val element = environment.driver.findElement(by(selector))
        element.sendKeys(evaluate(text).get.toString)
        environment.setCurrentTargetElement(element)
        logger.info(s"fill $selector with $text")
        Value.nullValue
      }
      case Submit => {
        environment.getCurrentTargetElement.foreach { element =>
          try {
            element.submit()
            environment.clearCurrentTargetElement()
          } catch {
            case e: StaleElementReferenceException => {
              logger.error("The element is stale")
            }
          }
        }
        logger.info(s"submit")
        Value.nullValue
      }
      case Click(selector) => {
        val element = environment.driver.findElement(by(selector))
        environment.setCurrentTargetElement(element)
        element.click()
        logger.info(s"click $selector")
        Value.nullValue
      }
      case Select(text, selector) => {
        val element = environment.driver.findElement(by(selector))
        val s = new org.openqa.selenium.support.ui.Select(element)
        s.selectByValue(evaluate(text).get.toString)
        environment.setCurrentTargetElement(element)
        logger.info(s"select $text from $selector")
        Value.nullValue
      }
      case f @ FunctionDef(name, args, statements) => {
        environment.addFunction(name, f)
        Value.nullValue
      }
      case FunctionCall(name, args) => {

        def callUserFunc(name: Symbol, args: List[AST]): Value = {
          val f = environment.getFunction(name).getOrElse(throw new UndefinedFunctionException(name))
          val localEnvironment = new LocalEnvironment(environment, environment.scriptPath)
          f.args.zip(args).foreach {
            case (argName, argValue) =>
              evaluate(Assignment(argName, argValue))(localEnvironment)
          }
          evaluateStatements(f.statements)(localEnvironment)
        }

        lookupEmbeddedFunctions(name).map {
          f => f(args.map(arg => evaluate(arg)))
        } getOrElse {
          callUserFunc(name, args)
        }
      }
      case Close => {
        environment.driver.close()
        logger.info("close")
        Value.nullValue
      }
      case Require(file) => {
        val originalScriptPath = environment.scriptPath
        val scriptDir = originalScriptPath.getParentFile
        val f = new File(scriptDir, evaluate(file).get.toString)
        logger.info(s"Loading ${f.getAbsolutePath}")
        environment.scriptPath = f
        evaluateStatements(loadFile(f))(environment)
        environment.scriptPath = originalScriptPath
        Value.nullValue
      }
      case Assignment(name, value) => {
        environment.setVariable(name, evaluate(value), reassignment = false)
        Value.nullValue
      }
      case Reassignment(name, value) => {
        if (environment.hasVariable(name)) {
          environment.setVariable(name, evaluate(value), reassignment = true)
        } else {
          throw new UndefinedVariableException(name)
        }
        Value.nullValue
      }
      case Variable(name) => {
        environment.getVariable(name).getOrElse(throw new UndefinedVariableException(name))
      }
      case ListAccess(name, index) => {
        val array = environment.getVariable(name).getOrElse(throw new UndefinedVariableException(name))
        Value(AnyType, array.get.asInstanceOf[List[_]].apply(index))
      }
      case Foreach(list, name, statements) => {
        val localEnv = new LocalEnvironment(environment, environment.scriptPath)
        val lst = evaluate(list)
        if (lst.tpe != ListType) {
          throw new TypeException(s"$lst is not a list")
        }
        for {
          v <- lst.get.asInstanceOf[List[_]]
        } {
          localEnv.setVariable(name, Value(AnyType, v))
          evaluateStatements(statements)(localEnv)
        }
        Value.nullValue
      }
      case EqualOps(left, right) => {
        if (evaluate(left).get == evaluate(right).get) Value.trueValue else Value.falseValue
      }
      case LessOps(left, right) => {
        if (evaluate(left).get.toString.toInt < evaluate(right).get.toString.toInt)
          Value.trueValue
        else
          Value.falseValue
      }
      case If(cond, ifStatements, elseStatements) => {
        val bool = evaluate(cond) match {
          case c if c.tpe == BooleanType => c.get.asInstanceOf[Boolean]
          case _ => throw new TypeException("Condition must return boolean value")
        }
        if (bool) {
          evaluateStatements(ifStatements)
        } else {
          evaluateStatements(elseStatements)
        }
      }
      case Println(ast) => {
        println(evaluate(ast).get)
        Value.nullValue
      }
      case ShellExec(command) => {
        import scala.sys.process._

        // fix me
        var commandList: ListBuffer[ProcessBuilder] = ListBuffer.empty
        var subList: ListBuffer[String] = ListBuffer.empty
        for (token <- command) {
          if (token == "|") {
            commandList += Process(subList.toList)
            subList = ListBuffer.empty
          } else {
            subList += token
          }
        }
        commandList += Process(subList.toList)
        subList = ListBuffer.empty

        val process = commandList.reduceLeft { (x, y) =>
          x #| y
        }
        logger.debug("Execute: " + process)
        val out = process.lines.mkString(System.lineSeparator())
        Value(StringType, out)
      }
    }
  }

}
