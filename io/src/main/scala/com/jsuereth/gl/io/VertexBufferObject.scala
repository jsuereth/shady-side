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

package com.jsuereth.gl.io

import org.lwjgl.system.MemoryStack
import org.lwjgl.opengl.GL15.{
    GL_ARRAY_BUFFER,
    GL_ELEMENT_ARRAY_BUFFER,
    GL_STATIC_DRAW,
    glGenBuffers,
    glBindBuffer,
    glBufferData,
    glDeleteBuffers
}
import org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER
import java.nio.ByteBuffer

/**
 * Represents a vertex array object stored in graphics memory.
 *
 * The object stores vertex data of type VertexType.  The layout
 * is determined via the {{vaoAttributes[VertexType]}} method.
 */
trait VertexBufferObject extends GpuObject {
  /** The VBO id from OpenGL. */
  def id: Int
  /** The OpenGL buffer type. */
  protected def glBufferType: Int
  /** Binds this object into a VAO with its glBufferType. */
  def bind(): Unit =
    glBindBuffer(glBufferType, id)

  /** True if this VBO is an indices VBO. */
  final def isIndex: Boolean = glBufferType == GL_ELEMENT_ARRAY_BUFFER
  override def free(): Unit =
    glDeleteBuffers(id)
}

object VertexBufferObject {
  /** A class where the VBO has already been loaded.  This just tracks the type of buffer and how to clean it. */
  final class PreloadedVbo(override val id: Int, override val glBufferType: Int) extends VertexBufferObject {
    override def toString = s"VBO($id, ${bufferTypeToString(glBufferType)})"
  }


  // Helper method for pretty printing, TODO - make this an enum?
  def bufferTypeToString(tpe: Int): String =
    tpe match {
      case GL_ARRAY_BUFFER => "Vertex"
      case GL_ELEMENT_ARRAY_BUFFER => "Index"
      case GL_TEXTURE_BUFFER => "Texture"
      case _ => "Unknown"
    }

  inline def loadStatic[T: BufferLoadable](data: Seq[T])(using MemoryStack): VertexBufferObject = {
    val id = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, id)
    withLoadedBuf(data)(glBufferData(GL_ARRAY_BUFFER, _, GL_STATIC_DRAW))
    PreloadedVbo(id, GL_ARRAY_BUFFER)
  }

  inline def loadStaticIndex(data: Seq[Int])(using MemoryStack): VertexBufferObject = {
    val id = glGenBuffers()
    // Note: this implies we're going to bind to VAO as we load...
    // I think that's actually a dumb OpenGL requirement for the buffer, even if we bind
    // to multiple VAOs for the same buffer.
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id)
    withLoadedBuf(data)(glBufferData(GL_ELEMENT_ARRAY_BUFFER, _, GL_STATIC_DRAW))
    PreloadedVbo(id, GL_ELEMENT_ARRAY_BUFFER)
  }
}