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

trait AlpacaRuntimeException

class UndefinedFunctionException(name: Symbol) extends RuntimeException with AlpacaRuntimeException {

  override def getMessage: String = s"Undefined variable $name."

}

class UndefinedVariableException(variable: Symbol) extends RuntimeException with AlpacaRuntimeException {

  override def getMessage: String = s"Undefined variable $variable."

}

class TypeException(message: String) extends RuntimeException(message) with AlpacaRuntimeException {
}

class SyntaxError(message: String) extends Exception(message)

class UnexpectedException(message: String) extends RuntimeException(message)
