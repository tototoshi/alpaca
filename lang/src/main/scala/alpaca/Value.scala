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

sealed trait Tpe
case object BooleanType extends Tpe
case object StringType extends Tpe
case object IntType extends Tpe
case object UnitType extends Tpe
case object AnyType extends Tpe
case object ListType extends Tpe

case class Value(tpe: Tpe, get: Any)

object Value {
  val nullValue = Value(UnitType, {})
  def intValue(i: Int) = Value(IntType, i)
  val trueValue = Value(BooleanType, true)
  val falseValue = Value(BooleanType, false)

  def asInt(v: Value): Int = {
    v match {
      case Value(IntType, i) => i.asInstanceOf[Int]
      case Value(StringType, s) => s.asInstanceOf[String].toInt
      case _ => throw new TypeException(s"Can't convert ${v.get} to int")
    }
  }

  def asString(v: Value): String = v.get.toString

}

