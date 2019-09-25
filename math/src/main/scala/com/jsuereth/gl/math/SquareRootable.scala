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

/** Denotes that you can take "roots" for a type T. 
 *
 *  Effectively, this means we have the following functions:
 *  - root(value, n)
 *  - sqrt(value) // root(value,2)
 *  - pow(n, value) // n^value
 */ 
trait Rootable[T] {
  /** Computes the arbitrary root of a value. pow(root(value, base), base) == value */
  def root(value: T, base: T): T
  /** Computes the square root of a value. */
  def sqrt(value: T): T
  /** Exponential multiplication.  base^exp. */
  def pow(base: T, exp: T): T
}
object Rootable {
  given Rootable[Float] {
    // Approximation for arbitrary roots.
    def root(value: Float, base: Float): Float = pow(value, 1f / base)
    def sqrt(value: Float): Float = Math.sqrt(value.toDouble).toFloat
    def pow(base: Float, exp: Float): Float = Math.pow(base.toDouble, exp.toDouble).toFloat
  }
  given Rootable[Double] {
    // Approximation for arbitrary roots.
    def root(value: Double, base: Double): Double = Math.pow(Math.E, Math.log(base) / value.toDouble)
    def sqrt(value: Double): Double = Math.sqrt(value)
    def pow(base: Double, exp: Double): Double = Math.pow(base, exp)
  }
}

/** package-level method to compute sqrt on any type that's Rootable. */
def sqrt[T](value: T)(given r: Rootable[T]): T = r.sqrt(value)
/** package-level method to compute a root on any type that's Rootable. */
def root[T](value: T, base: T)(given r: Rootable[T]): T = r.root(value, base)
/** package-level method to compute the power on any type that's Rootable. */
def pow[T](base: T, exp: T)(given r: Rootable[T]): T = r.pow(base, exp)