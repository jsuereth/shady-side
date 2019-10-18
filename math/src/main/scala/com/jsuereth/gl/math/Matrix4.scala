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
type Matrix4[T] = Matrix4x4[T]
val Matrix4 = Matrix4x4

/** A matrix library for floating point values, meant to be relatively efficient and the analog of GLSL matrix. */
class Matrix4x4[T : ClassTag](val values: Array[T])
  // row-column formats
  def m11: T = values(0)
  def m21: T = values(1)
  def m31: T = values(2)
  def m41: T = values(3)
  def m12: T = values(4)
  def m22: T = values(5)
  def m32: T = values(6)
  def m42: T = values(7)
  def m13: T = values(8)
  def m23: T = values(9)
  def m33: T = values(10)
  def m43: T = values(11)
  def m14: T = values(12)
  def m24: T = values(13)
  def m34: T = values(14)
  def m44: T = values(15)


  def apply(row: Int)(col: Int): T =
    values(row + (col*4))

  def *(scale: T)(given Numeric[T]): Matrix4x4[T] =
    Matrix4x4(values map (_ * scale))

  def *(point: Vec4[T])(given Numeric[T]): Vec4[T] =
    import point.{x,y,z,w}
    val newX = m11*x + m12*y + m13*z + m14*w
    val newY = m21*x + m22*y + m23*z + m24*w
    val newZ = m31*x + m32*y + m33*z + m34*w
    val newW = m41*x + m42*y + m43*z + m44*w
    Vec4(newX, newY, newZ, newW)

  def *(other: Matrix4x4[T])(given Numeric[T]): Matrix4x4[T] =
    // TODO - attempt to use Coppersmith–Winograd algorithm?
    // For now we do this naively, which is possibly more efficeint.
    val next = new Array[T](16)
    def idx(row: Int, col: Int): Int = row + (col*4)
    // TODO - Should we unroll this?
    for
      i <- 0 until 4
      j <- 0 until 4
    do next(idx(i,j)) = (for
      k <- 0 until 4
    yield apply(i)(k) * other(k)(j)).sum
    Matrix4x4(next)

  def determinant(given Numeric[T]): T =
    // TODO - Use a better decomposition to compute the inverse!
    (m14*m23*m32*m41) - (m13*m24*m32*m41) - (m14*m22*m33*m41) + (m12*m24*m33*m41) +
      (m13*m22*m34*m41) - (m12*m23*m34*m41) - (m14*m23*m31*m42) + (m13*m24*m31*m42) +
      (m14*m21*m33*m41) - (m11*m24*m33*m42) - (m13*m21*m34*m42) + (m11*m23*m34*m42) +
      (m14*m22*m31*m43) - (m12*m24*m31*m43) - (m14*m21*m32*m43) + (m11*m24*m32*m43) +
      (m12*m21*m34*m43) - (m11*m22*m34*m43) - (m13*m22*m31*m44) + (m12*m23*m31*m44) +
      (m13*m21*m32*m44) - (m11*m23*m32*m44) - (m12*m21*m33*m44) + (m11*m22*m33*m44)
  def transpose: Matrix4x4[T] =
    new Matrix4x4[T](Array(
      m11, m12, m13, m14,
      m21, m22, m23, m24,
      m31, m32, m33, m34,
      m41, m42, m43, m44
    ))
  def inverse(given Fractional[T]): Matrix4x4[T] =
    // Stack overflow when we had 1.0f / determinant.
    val scale: T = summon[Fractional[T]].one / determinant
    // TODO - use a better decomposition to compute the inverse!
    val n11 = (m23*m34*m42) - (m24*m33*m42) + (m24*m32*m43) - (m22*m34*m43) - (m23*m32*m44) + (m22*m33*m44)
    val n12 = (m14*m33*m42) - (m13*m34*m42) - (m14*m32*m43) + (m12*m34*m43) + (m13*m32*m44) - (m12*m33*m44)
    val n13 = (m13*m24*m42) - (m14*m23*m42) + (m14*m22*m43) - (m12*m24*m43) - (m13*m22*m44) + (m12*m23*m44)
    val n14 = (m14*m23*m32) - (m13*m24*m32) - (m14*m22*m33) + (m12*m24*m33) + (m13*m22*m34) - (m12*m23*m34)
    val n21 = (m24*m33*m41) - (m23*m34*m41) - (m24*m31*m43) + (m21*m34*m43) + (m23*m31*m44) - (m21*m33*m44)
    val n22 = (m13*m34*m41) - (m14*m33*m41) + (m14*m31*m43) - (m11*m34*m43) - (m13*m31*m44) + (m11*m33*m44)
    val n23 = (m14*m23*m41) - (m13*m24*m41) - (m14*m21*m43) + (m11*m24*m43) + (m13*m21*m44) - (m11*m23*m44)
    val n24 = (m13*m24*m31) - (m14*m23*m31) + (m14*m21*m33) - (m11*m24*m33) - (m13*m21*m34) + (m11*m23*m34)
    val n31 = (m22*m34*m41) - (m24*m32*m41) + (m24*m31*m42) - (m21*m34*m42) - (m22*m31*m44) + (m21*m32*m44)
    val n32 = (m14*m32*m41) - (m12*m34*m41) - (m14*m31*m42) + (m11*m34*m42) + (m12*m31*m44) - (m11*m32*m44)
    val n33 = (m12*m24*m41) - (m14*m22*m41) + (m14*m21*m42) - (m11*m24*m42) - (m12*m21*m44) + (m11*m22*m44)
    val n34 = m14*m22*m31 - m12*m24*m31 - m14*m21*m32 + m11*m24*m32 + m12*m21*m34 - m11*m22*m34
    val n41 = m23*m32*m41 - m22*m33*m41 - m23*m31*m42 + m21*m33*m42 + m22*m31*m43 - m21*m32*m43
    val n42 = m12*m33*m41 - m13*m32*m41 + m13*m31*m42 - m11*m33*m42 - m12*m31*m43 + m11*m32*m43
    val n43 = m13*m22*m41 - m12*m23*m41 - m13*m21*m42 + m11*m23*m42 + m12*m21*m43 - m11*m22*m43
    val n44 = m12*m23*m31 - m13*m22*m31 + m13*m21*m32 - m11*m23*m32 - m12*m21*m33 + m11*m22*m33

    new Matrix4x4[T](Array[T](
      n11*scale, n21*scale, n31*scale, n41*scale,
      n12*scale, n22*scale, n32*scale, n42*scale,
      n13*scale, n23*scale, n33*scale, n43*scale,
      n14*scale, n24*scale, n34*scale, n44*scale
    ))

  def toMat3x3: Matrix3x3[T] =
    Matrix3x3(Array(m11, m21, m31, 
                    m12, m22, m32, 
                    m13, m23, m33))

  override def toString: String =
    def f(t: T): String = t.toString     // TODO - use fixed-format number width...
    s""":|${f(m11)}|${f(m12)}|${f(m13)}|${f(m14)}|
        :|${f(m21)}|${f(m22)}|${f(m23)}|${f(m24)}|
        :|${f(m31)}|${f(m32)}|${f(m33)}|${f(m34)}|
        :|${f(m41)}|${f(m42)}|${f(m43)}|${f(m44)}|""".stripMargin(':')
object Matrix4x4

  def identity[T: ClassTag : Numeric]: Matrix4x4[T] =
    new Matrix4x4(Array(
      one,  zero, zero, zero,
      zero, one,  zero, zero,
      zero, zero, one,  zero,
      zero, zero, zero, one
    ))

  /** Creates a scaling matrix with the given factors:
    * @param x the scale factor for the x dimension
    * @param y the scale factor for the y dimension
    * @param z the scale factory for the z dimension.
    */
  def scale[T : ClassTag : Numeric](x: T, y: T, z: T): Matrix4x4[T] =
    new Matrix4x4(Array(
      x,    zero, zero, zero,
      zero, y,    zero, zero,
      zero, zero, z,    zero,
      zero, zero, zero, one
    ))

  /**
    * Creates an affine transform matrix with transformations in x/y/z space.
    */
  def translate[T : ClassTag : Numeric](x: T, y: T, z: T): Matrix4x4[T] =
    new Matrix4x4(Array(
      one,  zero, zero, zero,
      zero, one,  zero, zero,
      zero, zero, one,  zero,
      x,    y,    z,    one
    ))
  /**
    * Creates a rotation matrix around the x axis
    */
  def rotateX[T: ClassTag : Trigonometry : Numeric](angle: T): Matrix4[T] =
    val r = angleToRadians(angle)
    Matrix4(Array(
      one,  zero,   zero,    zero,
      zero, cos(r), -sin(r), zero,
      zero, sin(r), cos(r),  zero,
      zero, zero,   zero,    one
    ))

  /** Creates a rotation matrix around the y axis */
  def rotateY[T: ClassTag : Trigonometry : Numeric](angle: T): Matrix4[T] =
    val r = angleToRadians(angle)
    Matrix4(Array(
      cos(r),  zero, sin(r), zero,
      zero,    one,  zero,   zero,
      -sin(r), zero, cos(r), zero,
      zero,    zero, zero,   one
    ))
  /** Creates a rotations matrix around the z axis */
  def rotateZ[T: ClassTag : Trigonometry : Numeric](angle: T): Matrix4[T] =
    val r = angleToRadians(angle)
    Matrix4(Array(
      cos(r), -sin(r), zero, zero,
      sin(r), cos(r),  zero, zero,
      zero,   zero,    one,  zero,
      zero,   zero,    zero, one
    ))

  /** 
   * Creates a 'view' matrix for a given eye. 
   *
   * @param at The location being looked at.
   * @param eye The location of the eye.
   * @param up The direction of up according to the eye.
   */
  def lookAt[T: ClassTag : Rootable : Fractional](at: Vec3[T], eye: Vec3[T], up: Vec3[T]): Matrix4[T] =
    val zaxis = (at - eye).normalize          // L  = C - E
    val xaxis = (zaxis cross up).normalize    // S  = L x U
    val yaxis = (xaxis cross zaxis).normalize // U' = S x L
    // TODO - can we auto-multiple the camera/eye matrix?
    Matrix4[T](Array(
      xaxis.x, yaxis.x, -zaxis.x,  zero,  // (S, 0)
      xaxis.y, yaxis.y, -zaxis.y,  zero,  // (U', 0)
      xaxis.z, yaxis.z, -zaxis.z,  zero,  // (-L, 0)
      zero,    zero,    zero,      one    // (-E, 1)
    )) * Matrix4x4.translate[T](-eye.x, -eye.y, -eye.z)
  /**
   * Creates a perspective projection matrix.
   * Based on gluPerspective.
   *
   * @param fovy - field of view for y (angle in degrees)
   * @param aspect - aspect ratio of the screen
   * @param zNear - distance from viewer to near clipping plane
   * @param zFar - distance from viewer to far clipping plane.
   */
  def perspective[T : ClassTag : Trigonometry : Fractional](fovy: T, aspect: T, zNear: T, zFar: T): Matrix4x4[T] =
    val f = (one / tan(angleToRadians(fovy / two)))
    val z = (zFar + zNear) / (zNear - zFar)
    val zFixer = (two*zFar*zNear)/(zNear - zFar)
    Matrix4(Array(
      f/aspect, zero, zero,  zero,
      zero,     f,    zero,  zero,
      zero,     zero, z,     zFixer,
      zero,     zero, -one,  zero
    ))

  /**
   * Creates an orthographic projection matrix.
   *
   * @param left  The left clipping plane (x)
   * @param right The right clipping plane (x)
   * @param top The top clipping plane (y)
   * @param bottom The bottom clipping plane (y)
   * @param near The near clipping plane (z)
   * @param far The far clipping plane (z)
   */
  def ortho[T : ClassTag : Fractional](left: T, right: T, bottom: T, top: T, near: T, far: T): Matrix4[T] =
    val a = two / (right - left)
    val b = two / (top - bottom)
    val c = -two / (far - near)

    val tx = -(right + left) / (right - left)
    val ty = -(top + bottom) / (top - bottom)
    val tz = -(far + near) / (far - near)

    Matrix4(Array(
      a,     zero, zero, tx,
      zero,  b,    zero, ty,
      zero,  zero, c,    tz,
      zero,  zero, zero, one))



