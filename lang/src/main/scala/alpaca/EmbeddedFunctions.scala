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

import org.openqa.selenium.{ StaleElementReferenceException, By }
import com.typesafe.scalalogging.slf4j.Logging

object EmbeddedFunctions extends Logging {

  def allAsMap: Map[Symbol, List[Value] => Environment => Value] = Map(
    'len -> len,
    'sleep -> sleep,
    'exit -> exit,
    'accept -> accept,
    'find -> find,
    'attr -> attr,
    'text -> text,
    'assert -> assert,
    'visit -> visit,
    'submit -> submit,
    'close -> close
  )

  def len(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('len, 1, args.size)
    }
    val arg = args(0)
    Value.intValue(arg.get.asInstanceOf[List[_]].size)
  }

  def sleep(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('sleep, 1, args.size)
    }
    Thread.sleep(Value.asInt(args(0)))
    Value.nullValue
  }

  def exit(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('exit, 1, args.size)
    }
    sys.exit(Value.asInt(args(0)))
  }

  def accept(args: List[Value])(environment: Environment): Value = {
    environment.driver.switchTo().alert().accept()
    Value.nullValue
  }

  def find(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('find, 1, args.size)
    }
    val selector = args(0)
    val elem = environment.driver.findElement(By.cssSelector(Value.asString(selector)))
    Value.webElementValue(elem)
  }

  def attr(args: List[Value])(environment: Environment): Value = {
    if (args.size != 2) {
      throw new InvalidArgumentSizeException('attr, 2, args.size)
    }
    val attrName = Value.asString(args(0))
    val elem = Value.asWebElement(args(1))
    Value.stringValue(elem.getAttribute(attrName))
  }

  def text(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('text, 1, args.size)
    }
    val elem = Value.asWebElement(args(0))
    Value.stringValue(elem.getText)
  }

  def assert(args: List[Value])(environment: Environment): Value = {
    if (args.size != 2) {
      throw new InvalidArgumentSizeException('attr, 2, args.size)
    }
    val x = Value.asString(args(0))
    val y = Value.asString(args(1))
    if (x != y) {
      println(s"Assertion failed: $x is not equal to $y.")
      environment.driver.close()
    }
    Value.nullValue
  }

  def close(args: List[Value])(environment: Environment): Value = {
    environment.driver.close()
    Value.nullValue
  }

  def visit(args: List[Value])(environment: Environment): Value = {
    if (args.size != 1) {
      throw new InvalidArgumentSizeException('visit, 1, args.size)
    }
    val url = Value.asString(args(0))
    environment.driver.get(url)
    environment.clearCurrentTargetElement()
    logger.info(s"visit $url")
    Value.nullValue
  }

  def submit(args: List[Value])(environment: Environment): Value = {
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

}
