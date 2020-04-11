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

final class Vec4[T : ClassTag](private[this] val values: Array[T]) {
  def x: T = values(0)
  def y: T = values(1)
  def z: T = values(2)
  def w: T = values(3)
  def xy: Vec2[T] = new Vec2[T](values)
  def rg: Vec2[T] = new Vec2[T](values)
  def xyz: Vec3[T] = new Vec3[T](values)
  def rgb: Vec3[T] = new Vec3[T](values)


  def +(other: Vec4[T])(using Numeric[T]): Vec4[T] = Vec4(x+other.x, y+other.y, z+other.z, w+other.w)
  def -(other: Vec4[T])(using Numeric[T]): Vec4[T] = Vec4(x-other.x, y-other.y, z-other.z, w-other.w)
  // behaves as GLSL would, just multiplies pairs...
  def *(other: Vec4[T])(using Numeric[T]): Vec4[T] = Vec4(x*other.x, y*other.y, z*other.z, w*other.w)
  // TODO - we may need to disambiguate this from the above overload.
  def *(quant: T)(using Numeric[T]): Vec4[T] = Vec4(x*quant, y*quant, z*quant, w*quant)
  /** Computes the dot product of this vector with another. */
  def dot(other: Vec4[T])(using Numeric[T]): T =
    (x*other.x) + (y*other.y) + (z*other.z) + (w*other.w)
  // TODO: cross

  /** the square of the length of this vector. */
  def lengthSquared(using Numeric[T]): T = this dot this
  /** Returns the length of this vector.  Requires a SQRT function. */
  def length(using Rootable[T], Numeric[T]): T =  sqrt(lengthSquared)

  /** Normalizes this vector (setting distance to 1).  Note:  This requires a valid SQRT function. */
  def normalize(using Fractional[T], Rootable[T]): Vec4[T] = {
    val l = length
    new Vec4[T](Array((x / l), (y / l), (y / l), (w / l)))
  }
  override def toString: String = s"($x, $y, $z, $w)"
}
object Vec4 {
  def apply[T: ClassTag](x: T, y: T, z: T, w: T): Vec4[T] = new Vec4[T](Array(x,y,z, w))
  def apply[T: ClassTag](xy: Vec2[T], z: T, w: T): Vec4[T] = new Vec4[T](Array(xy.x, xy.y, z, w))
  def apply[T: ClassTag](xyz: Vec3[T], w: T): Vec4[T] = new Vec4[T](Array(xyz.x, xyz.y, xyz.z, w))
}
