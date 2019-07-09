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
package io

import math._

import scala.compiletime.erasedValue
import deriving.Mirror

/** 
 * Returns the bytes required to store a given value in GLSL. 
 * Note: This does NOT work on dynamic data.  It returns a staticly known size for a type,
 *       assuming it is packed w/ a BufferLoadable typeclass.
 */
inline def sizeOf[T]: Int = inline erasedValue[T] match {
    case _: Boolean => 1
    case _: Byte => 1
    case _: Short => 2
    case _: Int => 4
    case _: Long => 8
    case _: Float => 4
    case _: Double => 8
    case _: Vec2[t] => 2*sizeOf[t]
    case _: Vec3[t] => 3*sizeOf[t]
    case _: Vec4[t] => 4*sizeOf[t] 
    case _: Matrix4x4[t] => 16*sizeOf[t]
    case _: Matrix3x3[t] => 9*sizeOf[t]
    // TODO - SizedArray opaque type...
    case _: (a *: b) => sizeOf[a]+sizeOf[b]
    case _: Product => implicit match {
        case m: Mirror.ProductOf[T] => sizeOf[m.MirroredElemTypes]
        case _ => compiletime.error("Product is not plain-old-data, cannot compute the type.")
    }
    // Note: Unit shows up when we decompose tuples with *:
    case _: Unit => 0
    case _ => compiletime.error("Cannot discover the binary size of type T.")
}

import org.lwjgl.opengl.GL11.{
  GL_FLOAT,
  GL_INT,
  GL_UNSIGNED_INT, // TODO
  GL_SHORT,
  GL_UNSIGNED_SHORT, // TODO
  GL_BYTE,
  GL_UNSIGNED_BYTE,  // TODO
  GL_DOUBLE
}
/** 
 * Calculates the OpenGL enum for a type we'll pass into a VAO. 
 * Note: For types like vec2 or mat4x4, this will return the primitive type,
 *       as OpenGL specifies these with primitive + size.
 */
inline def glType[T]: Int = inline erasedValue[T] match {
    // TODO - Modern OpenGL types: GL_FIXED, GL_HALF_FLOAT,GL_INT_2_10_10_10_REV, GL_UNSIGNED_INT_2_10_10_10_REV, GL_UNSIGNED_INT_10F_11F_11F_REV
    case _: Boolean => GL_BYTE
    case _: Byte => GL_BYTE
    case _: Short => GL_SHORT
    case _: Int => GL_INT
    case _: Float => GL_FLOAT
    case _: Double => GL_DOUBLE
    case _: Vec2[t] => glType[t]
    case _: Vec3[t] => glType[t]
    case _: Vec4[t] => glType[t]
    case _: Matrix4x4[t] => glType[t]
    case _: Matrix3x3[t] => glType[t]
    case _ => compiletime.error("This type is not a supported OpenGL type")
}

/**
 * Calculates the size for OpenGL vertex attribute calls.
 * Essentially just how many of a give primitive are stored in a type.
 */
inline def glSize[T]: Int = inline erasedValue[T] match {
    case _: Vec2[t] => 2
    case _: Vec3[t] => 3
    case _: Vec4[t] => 4
    case _: Matrix3x3[t] => 9
    case _: Matrix4x4[t] => 16
    case _: Boolean => 1
    case _: Byte => 1
    case _: Short => 1
    case _: Int => 1
    case _: Float => 1
    case _: Double => 1
    case _ => compiletime.error("This type is not a supported OpenGL type")
}

/** Grab the VAO attributes for a member of a case class. */
inline def attr[T](idx: Int, stride: Int, offset: Int): VaoAttribute =
  VaoAttribute(idx, glSize[T], glType[T], stride, offset)

/** Defines the vertex attribute pointer configuration we need for a VAO. */
case class VaoAttribute(idx: Int, size: Int, tpe: Int, stride: Int, offset: Int) {
  import org.lwjgl.opengl.GL20.{glVertexAttribPointer,glEnableVertexAttribArray}
  /** Sends open-gl the VAO attribute pointer information given the index of this attribute. */
  def define(): Unit =
      glVertexAttribPointer(idx, size, tpe, false, stride, offset)
  def enable(): Unit =
         glEnableVertexAttribArray(idx)

  override def toString: String = {
     val tpeString = tpe match {
         case GL_FLOAT => "float"
         case _ => "unknown"
     }
     s"$idx(type: $tpeString size: $size, stride: $stride, offset: $offset)"
  }
}

/** 
 * Attempts to construct an array of VAO attribute definitions for a loaded VAO. 
 *
 * - Calculates stride size via total byte-size of T
 * - Caclulates offsets via incrementally adding each member of T
 * - Automatically determines GL types of members using glType[m]
 * - Automatically determines GL size information using glSize[m]
 */
inline def vaoAttributes[T]: Array[VaoAttribute] = {
  val stride = sizeOf[T]
  inline erasedValue[T] match {
    case _: Tuple1[a] =>    Array(attr[a](0,stride, 0))
    // TODO - use a continuation approach to tease apart tuples, like we do in sizeOf.
    case _: (a, b) =>       Array(attr[a](0, stride, 0),
                                  attr[b](1, stride, sizeOf[a]))
    case _: (a, b, c) =>    Array(attr[a](0, stride, 0),
                                  attr[b](1, stride, sizeOf[a]),
                                  attr[c](2, stride, sizeOf[a]+sizeOf[b]))
    case _: (a, b, c, d) => Array(attr[a](0, stride, 0),
                                  attr[b](1, stride, sizeOf[a]),
                                  attr[c](2, stride, sizeOf[a]+sizeOf[b]),
                                  attr[d](3, stride, sizeOf[a]+sizeOf[b]+sizeOf[c]))
    case _: Product =>
      delegate match {
        case m: Mirror.ProductOf[T] => vaoAttributes[m.MirroredElemTypes]
      }
    case _ => compiletime.error("Cannot compute the VAO attributes of this type.")
  }
}