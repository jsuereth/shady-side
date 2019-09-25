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

import scala.reflect.ClassTag

// Aliases
type Matrix3[T] = Matrix3x3[T]
val Matrix3 = Matrix3x3

/** A matrix library for floating point values, meant to be relatively efficient and the analog of GLSL matrix. */
class Matrix3x3[T : ClassTag](val values: Array[T]) {
  // row-column formats
  def m11: T = values(0)
  def m21: T = values(1)
  def m31: T = values(2)
  def m12: T = values(3)
  def m22: T = values(4)
  def m32: T = values(5)
  def m13: T = values(6)
  def m23: T = values(7)
  def m33: T = values(8)


  def apply(row: Int)(col: Int): T =
    values(row + (col*3))

  def *(scale: T)(given Numeric[T]): Matrix3x3[T] =
    Matrix3x3(values map (_ * scale))

  def *(vec: Vec3[T])(given Numeric[T]): Vec3[T] = {
    import vec.{x,y,z}
    val newX = m11*x + m12*y + m13*z
    val newY = m21*x + m22*y + m23*z
    val newZ = m31*x + m32*y + m33*z
    Vec3(newX, newY, newZ)
  }
  def *(other: Matrix3x3[T])(given Numeric[T]): Matrix3x3[T] = {
    // TODO - attempt to use Coppersmith–Winograd algorithm?
    // For now we do this naively, which is possibly more efficeint.
    val next = new Array[T](9)
    def idx(row: Int, col: Int): Int = row + (col*3)
    // TODO - Should we unroll this?
    for {
      i <- 0 until 3
      j <- 0 until 3
    } next(idx(i,j)) = (for {
      k <- 0 until 3
    } yield apply(i)(k) * other(k)(j)).sum
    Matrix3x3(next)
  }
  // TODO - Derive "show" or "print" across all numbers/formats once we have a shapeless release.
  override def toString: String = {
    def f(t: T): String = t.toString     // TODO - use fixed-format number width...
    s""":|${f(m11)}|${f(m12)}|${f(m13)}|
        :|${f(m21)}|${f(m22)}|${f(m23)}|
        :|${f(m31)}|${f(m32)}|${f(m33)}|""".stripMargin(':')
  }
}
object Matrix3x3 {
  def identity[T: ClassTag : Numeric]: Matrix3x3[T] =
    new Matrix3x3(Array(
      one,  zero, zero,
      zero, one,  zero,
      zero, zero, one,
    ))
}