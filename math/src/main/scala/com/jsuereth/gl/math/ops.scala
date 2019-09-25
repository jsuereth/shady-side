/*
 * Copyright 2019 Google LLC
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

package com.jsuereth.gl.math

import scala.math.Numeric
import scala.math.Fractional


// Remove implicit conversion warning....

given MyNumericOps: {
  def (x: T) +[T](y: T)(given n: Numeric[T]): T = n.plus(x,y)
  def (x: T) -[T](y: T)(given n: Numeric[T]): T = n.minus(x,y)
  def (x: T) *[T](y: T)(given n: Numeric[T]): T = n.times(x,y)
  def (x: T) unary_-[T](given n: Numeric[T]): T = n.negate(x)
}
given MyFractionalOps: {
  def (x: T) /[T](y: T)(given f: Fractional[T]): T = f.div(x,y)
}

def zero[T: Numeric]: T = summon[Numeric[T]].zero
def one[T: Numeric]: T = summon[Numeric[T]].one
def two[T: Numeric]: T = one + one
