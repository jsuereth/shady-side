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
import org.lwjgl.opengl.GL11.{
    GL_TRIANGLES,
    GL_UNSIGNED_INT,
    glDrawArrays,
    glDrawElements
}
import org.lwjgl.opengl.GL15.{
    GL_ARRAY_BUFFER,
    GL_ELEMENT_ARRAY_BUFFER,
    GL_STATIC_DRAW,
    glGenBuffers,
    glBindBuffer,
    glBufferData
}
import org.lwjgl.opengl.GL20.{
  glEnableVertexAttribArray
}
import org.lwjgl.opengl.GL30.{
  glGenVertexArrays,
  glDeleteVertexArrays,
  glBindVertexArray
}
import java.nio.ByteBuffer

/**
 * Represents a vertex array object stored in graphics memory.
 *
 * The object stores vertex information via VertexBufferObjects.
 *
 * These may be attached to a VAO.
 */
final class VertexArrayObject(id: Int) extends GpuObject {
  def bind(): Unit = glBindVertexArray(id)
  /** Removes this VAO from being used. */
  def unbind(): Unit = glBindVertexArray(0)
  /** The set of all attached VBOs. */
  private val attached = collection.mutable.ArrayBuffer[Attached]()
  private var renderWithIndices: Boolean = false

  /** This class represents a VBO that has been attached to this VAO. */
  final case class Attached(vbo: VertexBufferObject, attributes: Array[VaoAttribute]) {
    /** Bind the attributes of a VBO in a VAO prior to usage. */
    def bindAttributes(): Unit = 
      for {
        a <- attributes
      } glEnableVertexAttribArray(a.idx)
    def hasAttributes: Boolean = !attributes.isEmpty

    override def toString(): String = s"$vbo[${attributes.mkString(", ")}]"
  }

  /** 
   * Attaches the given VBO to this VAO, using the speciified attribtues. 
   * 
   * Note: this does NOT ensure uniqueness of attributes.
   */
  def attach(vbo: VertexBufferObject, attributes: Array[VaoAttribute] = Array.empty): Attached = {
    bind()
    vbo.bind()
    attributes.foreach(_.define())
    renderWithIndices = renderWithIndices || vbo.isIndex
    // TODO - do we store the VAO?
    val result = Attached(vbo, attributes)
    attached.append(result)
    result
  }
  /** 
    * Send this VAO through shaders.
    *
    * This is a helper, and does not need to be used. Generally, clinets will know
    * better how to render data within their VAOs, but this method aids quick demos.
    *
    * Implementation notes:
    * 
    * - Assumes all index buffers are GL_UNSIGNED_INT
    * - Always draws from index 0 to count.
    * - Always binds the VAO prior to a `glDraw*` call
    * - Always unbinds
    */
  def draw(start: Int, count: Int, drawMode: Int = GL_TRIANGLES): Unit = {
    bind()
    attached.foreach(_.bindAttributes())
    if(renderWithIndices)  glDrawElements(drawMode, count, GL_UNSIGNED_INT, start)
    else glDrawArrays(drawMode, 0, count)
    unbind()
  }

  override def free(): Unit = {
    // TODO - ensure only-once removal...
    attached.foreach(_.vbo.free())
    glDeleteVertexArrays(id)
  }

  override def toString(): String = 
    s"VAO($id, attached=[${attached.mkString(", ")}])"
}
object VertexArrayObject {
    /** Loads a VAO whose elements will align with the passed in type.
     *  @tparam T the plain-old-data type (case class) where each member becomes one of the vertex attribute pointers in this VAO.
     *  @param vertices A sequence of all the verticies to load into graphics memory.
     *  @param indicies A set of indicies to refereence in the vertex array when drawing geometry.
     */
   inline def loadWithIndex[T : BufferLoadable](vertices: Seq[T], indices: Seq[Int])(using MemoryStack): VertexArrayObject = {
        val vao = VertexArrayObject(glGenVertexArrays())
        vao.bind()
        val attrs: Array[VaoAttribute] = vaoAttributes[T]
        // Create the Vertex data
        val vertexBuf = VertexBufferObject.loadStatic(vertices)
        vao.attach(vertexBuf, attrs)
        // Now create the index buffer.
        val indexBuf = VertexBufferObject.loadStaticIndex(indices)
        vao.attach(indexBuf)
        vao
    }
    /** Loads a VAO whose elements will align with the passed in type.
     *  @tparam T the plain-old-data type (case class) where each member becomes one of the vertex attribute pointers in this VAO.
     *  @param vertices A sequence of all the verticies to load into graphics memory.
     */
    inline def loadRaw[T : BufferLoadable](vertices: Seq[T])(using MemoryStack): VertexArrayObject = {
        val vao = VertexArrayObject(glGenVertexArrays())
        vao.bind()
        val attrs: Array[VaoAttribute] = vaoAttributes[T]
        // Create the Vertex data
        val vertexBuf = VertexBufferObject.loadStatic(vertices)
        vao.attach(vertexBuf, attrs)
        vao
    }
}