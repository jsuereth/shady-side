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
 * The object stores vertex data of type VertexType.  The layout
 * is determined via the {{vaoAttributes[VertexType]}} method.
 */
trait VertexArrayObject[VertexType] extends GLBuffer {
    /** Binds this VAO for usage. */
    def bind(): Unit = glBindVertexArray(id)
    /** Removes this VAO from being used. */
    def unbind(): Unit = glBindVertexArray(0)
    /** Renders the VAO through a shader pipeline. */
    def draw(): Unit

    // TODO - Clean VBOs?
    override def close(): Unit = glDeleteVertexArrays(id)
}
object VertexArrayObject {
    /** Loads a VAO whose elements will align with the passed in type.
     *  @tparam T the plain-old-data type (case class) where each member becomes one of the vertex attribute pointers in this VAO.
     *  @param vertices A sequence of all the verticies to load into graphics memory.
     *  @param indicies A set of indicies to refereence in the vertex array when drawing geometry.
     *
     *  Note: This assumes TRIANGLES are being sent for drawing.
     */
    inline def loadWithIndex[T : BufferLoadable](vertices: Seq[T], indices: Seq[Int])(using MemoryStack): VertexArrayObject[T] = {
        val id = glGenVertexArrays()
        glBindVertexArray(id)
        val attrs: Array[VaoAttribute] = vaoAttributes[T]
        // Create the Vertex data
        val vboVertex = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboVertex)
        withLoadedBuf(vertices)(glBufferData(GL_ARRAY_BUFFER, _, GL_STATIC_DRAW))
        attrs.foreach(_.define())
        // Now create the index buffer.
        val vboIndex = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIndex)
        withLoadedBuf(indices)(glBufferData(GL_ELEMENT_ARRAY_BUFFER, _, GL_STATIC_DRAW))
        IndexedVertexArrayObject(id, attrs, indices.size)
    }
    /** Loads a VAO whose elements will align with the passed in type.
     *  @tparam T the plain-old-data type (case class) where each member becomes one of the vertex attribute pointers in this VAO.
     *  @param vertices A sequence of all the verticies to load into graphics memory.
     *
     *  Note: This assumes TRIANGLES are being sent for drawing.
     */
    inline def loadRaw[T : BufferLoadable](vertices: Seq[T])(using MemoryStack): VertexArrayObject[T] = {
        val id = glGenVertexArrays()
        val vertByteSize = sizeOf[T]*vertices.size
        val attrs = vaoAttributes[T]
        glBindVertexArray(id)
        val vboVertex = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboVertex)
        withLoadedBuf(vertices)(glBufferData(GL_ARRAY_BUFFER, _, GL_STATIC_DRAW))
        attrs.foreach(_.define())
        RawVertexArrayObject(id, attrs, vertices.size)
    }
}
/** This class can render a VAO where vertex data is rendered in order. */
class RawVertexArrayObject[VertexType](override val id: Int, 
                                    attrs: Array[VaoAttribute], 
                                    vertexCount: Int,
                                    drawMode: Int = GL_TRIANGLES) extends VertexArrayObject[VertexType] {
  def draw(): Unit = withBound {
    attrs.iterator.map(_.idx).foreach(glEnableVertexAttribArray(_))  
    glDrawArrays(drawMode, 0, vertexCount)
  }
  override def close(): Unit = glDeleteVertexArrays(id)
}

/** This class can render a VAO where you load vertex data separately from index data. */
class IndexedVertexArrayObject[VertexType](override val id: Int,
                                        attrs: Array[VaoAttribute],
                                        indexCount: Int,
                                        drawMode: Int = GL_TRIANGLES) extends VertexArrayObject[VertexType] {
  def draw(): Unit = withBound {
    attrs.iterator.map(_.idx).foreach(glEnableVertexAttribArray(_))
    // TODO openGL index type...
    glDrawElements(drawMode, indexCount, GL_UNSIGNED_INT, 0)
  }
}