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

// TODO - look into making this an opaque type w/ wrappers.  We give up == + toString...
// TODO - we probably need to provide our own set of numeric classes for this use case.
final class Vec3[T : ClassTag](private[this] val values: Array[T]) {
  /** The x coordinate value. */
  def x: T = values(0)
  /** The y coordinate value. */
  def y: T = values(1)
  /** The z coordinate value. */
  def z: T = values(2)

  /** Piecewise sum this vector with another. */
  def +(other: Vec3[T]) given Numeric[T]: Vec3[T] = Vec3(x+other.x, y+other.y, z+other.z)
  /** Piecewise subtract this vector with another. */
  def -(other: Vec3[T]) given Numeric[T]: Vec3[T] = Vec3(x-other.x, y-other.y, z-other.z)
  /** Piecewise multiple this vector with another. */
  def *(other: Vec3[T]) given Numeric[T]: Vec3[T] = Vec3[T](x*other.x, y*other.y, z*other.z)
  /** Multiple a given value against each element of the vector. */
  def *(quant: T) given Numeric[T]: Vec3[T] = Vec3(x*quant, y*quant, z*quant)


  /** Negates the value of this vector. */
  def negate given Numeric[T]: Vec3[T] = Vec3(-x,-y,-z)

  /** Computes the dot product of this vector with another. */
  def dot(other: Vec3[T]) given Numeric[T]: T =
    (x*other.x) + (y*other.y) + (z*other.z)

  /** Cross product with another vector. */
  def cross(other: Vec3[T]) given Numeric[T]: Vec3[T] =
    Vec3(
      (y*other.z) - (z*other.y),
      (z*other.x) - (x*other.z),
      (x*other.y) - (y*other.x)
    )

  /** Returns the square of the length of this vector.  More efficient to compute than length. */
  def lengthSquared given Numeric[T]: T = this dot this
  /** Returns the length of this vector.  Requires a SQRT function. */
  def length given Rootable[T], Numeric[T]: T =  sqrt(lengthSquared)

  /** Normalizes this vector.  Note:  This requires a valid SQRT function. */
  def normalize given Fractional[T], Rootable[T]: Vec3[T] =
    new Vec3[T](Array((x / length), (y / length), (z / length)))


  // GLSL compat
  def xy: Vec2[T] = new Vec2[T](values)
  def xyz: Vec3[T] = this
  def rg: Vec2[T] = new Vec2[T](values)
  def rgb: Vec3[T] = this


  // TODO - Convert/cast between types.  Should piece-wise convert.
  def as[U : ClassTag]: Vec3[U] = ???

  override def toString: String = s"($x,$y,$z)"
  override def equals(o: Any): Boolean =
    o match {
      case other: Vec3[T] => (x == other.x) && (y == other.y) && (z == other.z)
      case _ => false
    }

  // TODO - hashcode the contents of the array.
  override def hashCode(): Int = super.hashCode()
}
object Vec3 {
  def apply[T: ClassTag](x: T, y: T, z: T): Vec3[T] = new Vec3[T](Array(x,y,z))
  def apply[T: ClassTag](xy: Vec2[T], z: T): Vec3[T] = new Vec3[T](Array(xy.x, xy.y, z))
  // TODO - type-generic versions?
  def zero = Vec3(0,0,0)
  def up = Vec3(0,1,0)
  def down = Vec3(0,-1,0)
  def right = Vec3(1,0,0)
  def left = Vec3(-1,0,0)
  def forward = Vec3(0,0,-1)
  def back = Vec3(0,0,1)
}