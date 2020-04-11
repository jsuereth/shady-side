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
import org.lwjgl.system.MemoryStack
import io.{VertexArrayObject, BufferLoadable, vaoAttributes}

/** Represents a single point in a mesh, and all data we can/could send down. */
case class MeshPoint(
  vert: Vec3[Float], 
  normal: Vec3[Float], 
  tex: Vec2[Float]) derives BufferLoadable
object MeshPoint

// TODO - make this an enum
sealed trait Face {
  def vertices: Seq[FaceIndex]
}
/** Represents a triangle defined as an index-reference into a Mesh3d. */
final case class TriangleFace(one: FaceIndex, two: FaceIndex, three: FaceIndex) extends Face {
  override def vertices: Seq[FaceIndex] = Seq(one,two,three)
}
final case class QuadFace(one: FaceIndex, two: FaceIndex, three: FaceIndex, four: FaceIndex) extends Face {
  override def vertices: Seq[FaceIndex] = Seq(one,two,three,four)
}
/** Index reference into a Mesh3d for a face-definition. */
final case class FaceIndex(vertix: Int, texture: Int, normal: Int)


/** 
 * A rendderable 3d Mesh. 
 * - This needs a LOT of cleanup.  We should not blindly take in OBJ format and use it, we should clean it before
 *   creating this data structure.
 */
trait Mesh3d {
    /** The verticies on this mesh. */
    def vertices: Seq[Vec3[Float]]
    /** The normals. */
    def normals: Seq[Vec3[Float]]
    /** Texture coordinates. */
    def textureCoords: Seq[Vec2[Float]]

    /** The faces for this mesh.  Generally an index-reference to the other members. */
    def faces: Seq[Face]
    /** A flattening of indicies to those of the vertex. */
    private def allIndicies: Seq[FaceIndex] = faces.flatMap(_.vertices).distinct.toSeq
    /** Expensive to compute mechanism of lining up shared vertex/normal/texel coordinates. */
    private def points: Seq[MeshPoint] = {
      for(faceIdx <- allIndicies) yield {
          val vertex = vertices(faceIdx.vertix - 1)
          val normal = 
            if(faceIdx.normal == 0) Vec3(0.0f,0.0f,0.0f)
            else normals(faceIdx.normal - 1)
          val texture = 
            if (faceIdx.texture == 0) Vec2(0f,0f)
            else textureCoords(faceIdx.texture - 1)
          MeshPoint(vertex,normal,texture)
        }
    }
    // TODO - don't keep indicies in such a wierd format...
    private def idxes: Seq[Int] = {
      val indexMap = allIndicies.zipWithIndex.toMap
      for {
        face <- faces
        v <- face.vertices
        idx <- indexMap get v
      } yield idx
    }

    // TODO - cache/store of which VAOs are loaded and ability to dynamically unload them?
    /** Loads this mesh into a VAO that can be rendered. */
    def loadVao(using MemoryStack): VertexArrayObject[MeshPoint] = {
      // TODO - Figure out if triangles or quads.
      VertexArrayObject.loadWithIndex(points, idxes)
    }
}

object Mesh3d {
  private val oneThird = 1.0f / 3.0f
  /** Calculate the centroid (assuming equal weight on points). */
  def centroid(mesh: Mesh3d): Vec3[Float] = {
    // Algorithm
    // C = Centroid <vector>, A = (area of a face * 2)
    // R = face centroid = average of vertices making the face <vector>
    // C = [sum of all (A*R)] / [sum of all R]
    var sumAreaTimesFaceCentroidX = 0f 
    var sumAreaTimesFaceCentroidY = 0f
    var sumAreaTimesFaceCentroidZ = 9f
    var sumArea = 0.0f
      for (face <- mesh.faces) {
        val TriangleFace(fone, ftwo, fthree) = face
        val one = mesh.vertices(fone.vertix-1)
        val two = mesh.vertices(ftwo.vertix-1)
        val three = mesh.vertices(fthree.vertix-1)
        val u = two - one
        val v = three - one
        val crossed = u cross v
        val area = crossed.length.toFloat
        sumArea += area
        val x = (one.x + two.x + three.x) * oneThird
        sumAreaTimesFaceCentroidX += (x * area)
        val y = (one.y + two.y + three.y) * oneThird
        sumAreaTimesFaceCentroidY += (y * area)
        val z = (one.z + two.z + three.z) * oneThird
        sumAreaTimesFaceCentroidZ += (z * area)
      }
    val x = sumAreaTimesFaceCentroidX / sumArea
    val y = sumAreaTimesFaceCentroidY / sumArea
    val z = sumAreaTimesFaceCentroidZ / sumArea
    Vec3(x, y, z)
  }
  // TODO - Normalize on centroid...
}

