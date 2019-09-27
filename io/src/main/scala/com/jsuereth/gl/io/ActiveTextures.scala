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

import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL13.{
    GL_TEXTURE0,
    glActiveTexture
}
import org.lwjgl.opengl.GL20.{
  glUniform1i,
  GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS
}
import scala.collection.mutable.ArrayBuffer

/** A type representing an ActiveTextureIndex.  Normally in opengl these are GL_TEXTURE* enums. */
opaque type ActiveTextureIndex = Int

/** Convenience method to turn an index into a GL_TEXTUREN enuma nd call glActiveTexture. */
def activate(idx: ActiveTextureIndex): Unit =  glActiveTexture(GL_TEXTURE0 + idx)
/** Convenience method to bind the index of a texture to a uniform variable. */
def loadTextureUniform(location: Int, idx: ActiveTextureIndex): Unit = glUniform1i(location, unwrap(idx))
/** The maximum value of active indicies. */
def MaxActiveIndex: ActiveTextureIndex = glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS)

// Internal helper to lift things into/out of opque type.
private def unwrap(idx: ActiveTextureIndex): Int = idx
private def wrap(idx: Int): ActiveTextureIndex = idx

/** This interfaces describes which textures are bound, and gives access to binding IDs
 *  that can be used to bind more textures.
 *
 *  This is a mutable interface, as we expect it to be passe dthrough shader loadable context.
 */
trait ActiveTextures {
  /** Grabs the next available texture index. */  
  def acquire(): ActiveTextureIndex
  /** Returns a texture index in this call. */
  def release(idx: ActiveTextureIndex): Unit

  /** Acquires an active texture for a task,t hen releases it. */
  inline def withNextActiveIndex[A](f: ActiveTextureIndex => A): A = {
    val idx = acquire()
    try f(idx)
    finally release(idx)
  }
  /** Create a new "record" of what textures were used. */
  def push(): Unit
  /** Automatically release all textures used since last push. */
  def pop(): Unit
}
object ActiveTextures {
    def apply(maxIndex: Int = unwrap(MaxActiveIndex)): ActiveTextures = SimpleActiveTextures(maxIndex)
}

private class SimpleActiveTextures(maxIndex: Int) extends ActiveTextures {
    private var depth: Int = 0
    private val MaxDepth = 100
    private val used: ArrayBuffer[Int] = (0 until maxIndex).map(_ => 100).to(ArrayBuffer)
    /** Create a new "record" of what textures were used. */
    def push(): Unit = depth = Math.max(MaxDepth, depth+1)
    /** Open up all textures for usage from the last "pop" */
    def pop(): Unit = depth = Math.max(0, depth-1)
    def release(idx: ActiveTextureIndex): Unit = used(unwrap(idx)) = MaxDepth
    // TODO - ring buffer impl, something faster?
    def acquire(): ActiveTextureIndex =
      used.iterator.zipWithIndex.find((value, idx) => value > depth) match {
        case Some((_, idx)) => 
          used(idx) = depth
          wrap(idx)
        case None => throw RuntimeException("No more textures available!")
      }
}