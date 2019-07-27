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

/** An implementation of trigonometry functions for a type. */
trait Trigonometry[T] {
  /** Accepts a value in radians and returns the sine. */
  def sin(value: T): T
  /** Accepts a value in radians and returns the cosine. */
  def cos(value: T): T
  /** Accepts a value in radians and returns the tangent. */
  def tan(value: T): T

  // TODO - asin,acos,atan,atan2


  /** Converts from Degrees to Radians for a given type. */
  def angleToRadians(value: T): T
}
/** Accepts a value in radians and returns the sine. */
def sin[T](value: T) given (t: Trigonometry[T]): T = t.sin(value)
/** Accepts a value in radians and returns the cosine. */
def cos[T](value: T) given (t: Trigonometry[T]): T = t.cos(value)
/** Accepts a value in radians and returns the tangent. */
def tan[T](value: T) given (t: Trigonometry[T]): T = t.tan(value)
/** Converts from Degrees to Radians for a given type. */
def angleToRadians[T](value: T)  given (t: Trigonometry[T]): T = t.angleToRadians(value)

object Trigonometry {
  // TODO - Fast lookup tables
  // For now we just provide java's implementation of trig.
  given JavaFloatTrig as Trigonometry[Float] {
    def sin(value: Float): Float = Math.sin(value.toDouble).toFloat
    def cos(value: Float): Float = Math.cos(value.toDouble).toFloat
    def tan(value: Float): Float = Math.tan(value.toDouble).toFloat
    private val conversionFactor = (scala.math.Pi / 180.0f).toFloat
    def angleToRadians(value: Float): Float = value * conversionFactor
  }
  given JavaDoubleTrig as Trigonometry[Double] {
    def sin(value: Double): Double = Math.sin(value)
    def cos(value: Double): Double = Math.cos(value)
    def tan(value: Double): Double = Math.tan(value)
    private val conversionFactor = (scala.math.Pi / 180.0f)
    def angleToRadians(value: Double): Double = value * conversionFactor
  }
}