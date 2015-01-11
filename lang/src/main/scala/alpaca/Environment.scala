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

import scala.collection.mutable.{ Map => MutableMap }
import org.openqa.selenium.{ WebDriver, WebElement }
import java.io.File
import com.typesafe.scalalogging._
import alpaca.AST.FunctionDef

trait Environment {

  val driver: WebDriver

  var scriptPath: File

  def getCurrentTargetElement: Option[WebElement]

  def setCurrentTargetElement(element: WebElement)

  def clearCurrentTargetElement(): Unit

  def hasFunction(name: Symbol): Boolean

  def getFunction(name: Symbol): Option[FunctionDef]

  def addFunction(name: Symbol, function: FunctionDef): Unit

  def hasVariable(name: Symbol): Boolean

  def getVariable(name: Symbol): Option[Value]

  def setVariable(name: Symbol, value: Value, reassignment: Boolean)

}

class GlobalEnvironment(val driver: WebDriver, var scriptPath: File) extends Environment {

  private var currentTargetElement: Option[WebElement] = None

  private val functions: MutableMap[Symbol, FunctionDef] = MutableMap.empty

  private val variables: MutableMap[Symbol, Value] = MutableMap.empty

  def getCurrentTargetElement: Option[WebElement] = currentTargetElement

  def setCurrentTargetElement(element: WebElement) = {
    currentTargetElement = Some(element)
  }

  def clearCurrentTargetElement(): Unit = currentTargetElement = None

  def hasFunction(name: Symbol): Boolean = {
    functions.contains(name)
  }

  def getFunction(name: Symbol): Option[FunctionDef] = functions.get(name)

  def addFunction(name: Symbol, function: FunctionDef): Unit = {
    functions(name) = function
  }

  def hasVariable(name: Symbol): Boolean = {
    variables.contains(name)
  }

  def getVariable(name: Symbol): Option[Value] = variables.get(name)

  def setVariable(name: Symbol, value: Value, reassignment: Boolean): Unit = {
    if (reassignment) {
      if (!hasVariable(name)) {
        throw new UndefinedVariableException(name)
      }
      variables(name) = value
    } else {
      variables(name) = value
    }

  }

}

class LocalEnvironment(private val parent: Environment, var scriptPath: File) extends Environment with LazyLogging {

  val driver: WebDriver = parent.driver

  private var currentTargetElement: Option[WebElement] = None

  private val functions: MutableMap[Symbol, FunctionDef] = MutableMap.empty

  private val variables: MutableMap[Symbol, Value] = MutableMap.empty

  def getCurrentTargetElement: Option[WebElement] = {
    parent.getCurrentTargetElement.orElse(currentTargetElement)
  }

  def setCurrentTargetElement(element: WebElement) = {
    currentTargetElement = Some(element)
  }

  def clearCurrentTargetElement(): Unit = {
    currentTargetElement = None
    parent.clearCurrentTargetElement()
  }

  def hasFunction(name: Symbol): Boolean = {
    functions.contains(name) || parent.hasFunction(name)
  }

  def getFunction(name: Symbol): Option[FunctionDef] = {
    functions.get(name).orElse(parent.getFunction(name))
  }

  def addFunction(name: Symbol, function: FunctionDef): Unit = {
    functions(name) = function
  }

  def hasVariable(name: Symbol): Boolean = {
    variables.contains(name) || parent.hasVariable(name)
  }

  def getVariable(name: Symbol): Option[Value] = {
    variables.get(name).orElse(parent.getVariable(name))
  }

  def setVariable(name: Symbol, value: Value, reassignment: Boolean = false): Unit = {
    if (reassignment) {
      if (variables.contains(name)) {
        variables(name) = value
      } else if (parent.hasVariable(name)) {
        parent.setVariable(name, value, true)
      } else {
        throw new UndefinedVariableException(name)
      }
    } else {
      variables(name) = value
    }
  }

}
