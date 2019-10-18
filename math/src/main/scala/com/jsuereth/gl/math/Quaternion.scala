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

import scala.reflect.ClassTag

object Quaternion
  /** Computes 1/2 in a given numeric type. */
  def half[T : Fractional]: T = one / two
  
  def identity[T : Fractional]: Quaternion[T] = Quaternion(one, zero, zero, zero)
  /** Generate a quaternion that represents rotation about a given vector. */
  def rotation[T: Fractional : Trigonometry](angle: T, vector: Vec3[T]): Quaternion[T] =
    rotationRad(angleToRadians(angle), vector)
  /** Generate a quaternion that represents rotation about a given vector. */
  def rotationRad[T : Fractional : Trigonometry](angle: T, vector: Vec3[T]): Quaternion[T] =
    val halfAngle = angle * half[T]
    val sinHalfAngle = sin(halfAngle)
    val w = cos(halfAngle)
    val x = (sinHalfAngle * vector.x)
    val y = (sinHalfAngle * vector.y)
    val z = (sinHalfAngle * vector.z)
    Quaternion(w,x,y,z)
  /** Converts a Vec3 into the equivalent quaternion. */
  def fromVector[T : Rootable : Fractional](v: Vec3[T]): Quaternion[T] =
    val vn = v.normalize
    Quaternion(zero, vn.x, vn.y, vn.z)
  
  // Convert from angular acceleration into Quaternion.
  def fromAngularVector[T : Fractional : Rootable : Trigonometry](w: Vec3[T]): Quaternion[T] =
    rotationRad(w.length, w.normalize)
    
  /** takes euler pitch/yaw/roll and turns it into a quaternion.   Angles in degrees. */  
  def fromEuler[T : Fractional : Rootable : Trigonometry](pitch: T, yaw: T, roll: T): Quaternion[T] =
    // Basically we create 3 Quaternions, one for pitch, one for yaw, one for roll
    // and multiply those together.
    // the calculation below does the same, just shorter
    val p = angleToRadians(pitch) * half[T]
    val y = angleToRadians(yaw) * half[T]
    val r = angleToRadians(roll) * half[T]
    val sinp = sin(p)
    val siny = sin(y)
    val sinr = sin(r)
    val cosp = cos(p)
    val cosy = cos(y)
    val cosr = cos(r)
    val qx = sinr * cosp * cosy - cosr * sinp * siny
    val qy = cosr * sinp * cosy + sinr * cosp * siny
    val qz = cosr * cosp * siny - sinr * sinp * cosy
    val qw = cosr * cosp * cosy + sinr * sinp * siny
    Quaternion(qw,qx,qy,qz).normalize
/**
 * A way of representing numbers that is ideal for preserving rotational precision and generating rotation matrices as needed.
 * 
 * w + x*i + y*j + z*k
 *
 * Note: While this can be representing with any type, some operations will require various aspects of those types, e.g.
 *       normalization requries having roots.
 */ 
case class Quaternion[T](w: T, x: T, y: T, z: T) {

  def +(other: Quaternion[T])(given Numeric[T]): Quaternion[T] =
    Quaternion(w+other.w, other.x+x, other.y+y, other.z+z)
    
  def -(other: Quaternion[T])(given Numeric[T]): Quaternion[T] =
    Quaternion(w-other.w, x-other.x, y-other.y, z-other.z)
        
  def *(scalar: T)(given Numeric[T]): Quaternion[T] =
    Quaternion(w*scalar, x*scalar, y*scalar, z*scalar)
    
  /** Hamilton product.  This is similar to multiplying two rotation matrices together. */
  def *(other:Quaternion[T])(given Numeric[T]): Quaternion[T] =
    val newW = w*other.w - x*other.x - y*other.y - z*other.z
    val newX = w*other.x + x*other.w + y*other.z - z*other.y
    val newY = w*other.y - x*other.z + y*other.w + z*other.x
    val newZ = w*other.z + x*other.y - y*other.x + z*other.w
    Quaternion(newW, newX, newY, newZ)
  
  // Defined as rotating the vector by this quaternion.
  def *(other: Vec3[T])(given ClassTag[T], Fractional[T], Rootable[T]): Vec3[T] =
    val result =
      this * Quaternion.fromVector(other) * this.conjugate
    Vec3(result.x, result.y, result.z)
  
  def conjugate(given Numeric[T]): Quaternion[T] =
     Quaternion(w, -x, -y, -z);
  
  def normSquared(given Numeric[T]): T = w*w + x*x + y*y + z*z
  
  def normalize(given Rootable[T], Fractional[T]): Quaternion[T] =
    val ns = normSquared
    // Here is  a danger mechanism to avoid crazy divisions
    // What we really want is a better "near" function for floating point numbers.
    //import scala.math.abs
    //if(abs(ns) > 0.001f && abs(ns - f.one) > 0.001f) {
      val factor = (one / sqrt(ns))
      Quaternion(w * factor, x * factor, y * factor, z * factor)
    //} else this
  
  def toMatrix(given ClassTag[T], Rootable[T], Ordering[T], Fractional[T]): Matrix4[T] = normalize.toMatrixFromNorm
  
  // This did not translate correctly...
  private def toMatrixFromNorm(given ClassTag[T], Ordering[T], Fractional[T]): Matrix4x4[T] =
    val Nq = normSquared
    val s = if summon[Ordering[T]].gt(Nq, zero) then two else zero
    val X = x*s; val Y = y*s; val Z = z*s
    val wX = w*X; val wY = w*Y; val wZ = w*Z
    val xX = x*X; val xY = x*Y; val xZ = x*Z
    val yY = y*Y; val yZ = y*Z; val zZ = z*Z
    
    Matrix4x4(Array(
      one-(yY+zZ), xY+wZ,       xZ-wY,        zero,
      xY-wZ,       one-(xX+zZ), yZ+wX,        zero,
      xZ+wY,       yZ-wX,       zero-(xX+yY), zero,
      zero,        zero,        zero,         one
    ))
  override def toString: String = s"$w + ${x}i + ${y}j + ${z}j"
}