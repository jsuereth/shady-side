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
package mesh

import math.{given _, _}
import io.VertexArrayObject
import org.lwjgl.system.MemoryStack
import io.{VertexArrayObject, BufferLoadable, vaoAttributes}

/** 
 * A quick hack to defer defining how materials
 * get used in shaders until later. 
 */
trait MeshRenderContext {
  def applyMaterial(material: RawMaterial): Unit
}

/** A mesh that has been loaded into OpenGL. */
trait RenderableMesh {
  def original: BakedMesh
  /** Fires this mesh to shaders. */
  def render(ctx: MeshRenderContext): Unit 
}

/** 
 * Loads a baked mesh into OpenGL.
 *
 * TODO - we want to preserve type information in this
 *        abstraction so ShaderDSL can use MeshPoint
 *        directly!  We loose everything when hitting
 *        our current VAO impl, and we're not even
 *        being that efficient.
 */
def load(mesh: BakedMesh)(using MemoryStack): RenderableMesh = {
  var rawIndicies = collection.mutable.ArrayBuffer.empty[Int]
  var currentPtr = 0
  val groups = for {
    group <- mesh.groups
  } yield {
    for(f <- group.faces)
    rawIndicies.addOne(f.a).addOne(f.b).addOne(f.c)    
    val g = RenderableMeshGroup(
      startPtr = currentPtr,
      count = group.faces.length*3,
      material = group.material
    )
    currentPtr += g.count*io.sizeOf[Int]
    g
  }  
  SimpleRenderableMesh(
    original = mesh, 
    vao = VertexArrayObject.loadWithIndex(mesh.points, rawIndicies.toSeq), 
    groups = groups.toArray)
}

/** The information we need to render a portion of
 *  a mesh.
 */
final class RenderableMeshGroup(
  /** 
   * An index/pointer to the start of this group.
   * (0-offset), in bytes.
   */
  val startPtr: Int,
  /**
   * The number of indicies to render. Should be
   * divisible by 3 to form triangled.
   */
  val count: Int,
  /** TODO - Figure out materials. */
  val material: RawMaterial
)

/** Simplest version of a loaded mesh. */
final class SimpleRenderableMesh(
    /** The mesh that's been loaded. */
    override val original: BakedMesh,
    /** The VAO holding the mesh. */
    vao: VertexArrayObject,
    /** The groups within the mesh we need to render. */
    groups: Array[RenderableMeshGroup])
    extends RenderableMesh {
  def render(ctx: MeshRenderContext): Unit = {
    for (group <- groups) {
      ctx.applyMaterial(group.material)
      vao.draw(
        start=group.startPtr,
        count=group.count
        /*,drawMode = GL_TRIANGLES*/
      )
    }
  }
}            