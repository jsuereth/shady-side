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

package com.jsuereth.gl
package math

import scala.math.Numeric
import scala.reflect.ClassTag

class Vec2[T : ClassTag](private[this] val values: Array[T]) {
  def x: T = values(0)
  def y: T = values(1)
  def rg: Vec2[T] = this
  def xy: Vec2[T] = this

  def +(other: Vec2[T])(given Numeric[T]): Vec2[T] = Vec2(x+other.x, y+other.y)
  def -(other: Vec2[T])(given Numeric[T]): Vec2[T] = Vec2(x-other.x, y-other.y)
  // behaves as GLSL would, just multiplies pairs...
  def *(other: Vec2[T])(given Numeric[T]): Vec2[T] = Vec2(x*other.x, y*other.y)

  /** The dot product of this vector and another. */
  def dot(other: Vec2[T])(given Numeric[T]): T = x*other.x + y*other.y

  /** the square of the length of this vector. */
  def lengthSquared(given Numeric[T]): T = this dot this
  /** Returns the length of this vector.  Requires a SQRT function. */
  def length(given Rootable[T], Numeric[T]): T =  sqrt(lengthSquared)

  /** Normalizes this vector (setting distance to 1).  Note:  This requires a valid SQRT function. */
  def normalize(given Fractional[T], Rootable[T]): Vec2[T] =
     new Vec2[T](Array((x / length), (y / length)))

  override def toString: String = s"($x,$y)"
}
object Vec2 {
  def apply[T: ClassTag](x: T, y: T): Vec2[T] = new Vec2(Array(x,y))
}