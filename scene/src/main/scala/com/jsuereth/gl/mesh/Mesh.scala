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

/** Represnets *indices* to `MeshPoint`s to form a triangle. */
case class MeshTriangle(
  /** Reference to the data at point a. */
  a: Int,
  /** index to data at point b. */
  b: Int,
  /** index to data at point c. */
  c: Int
)

/** Reprresents a group/object within a mesh. */
case class MeshGroup(
  /** The facces to be rendered for this group. */
  faces: Array[MeshTriangle],
  /** The Material to render for this portion of the mesh. */
  // TODO - better material and such.
  material: RawMaterial
) {
  override def toString(): String =
    s"MeshGroup(faceCount=${faces.length})"
}

/** 
 * A mesh that has been streamlined for loading into OpenGL.
 *
 * Baking requires the following:
 * - All points are in one VAO-indexable array
 * - All portions of the mesh are grouped in objects.
 * - TODO: centroids calculated.
 * - TODO: Bounding box/collision stuff
 */
case class BakedMesh(
  points: Array[MeshPoint],
  groups: Array[MeshGroup]
) {
  override def toString(): String =
    s"BakedMesh(pointCount=${points.length}, ${groups.mkString(", ")})"
}

val noMaterial =
  RawMaterial("none", BaseMaterial(), MaterialTextures())

/** This converts the raw OBJ file parse into something we can render. */
def bake(parse: parser.ParsedObj): BakedMesh = {
  val bakedPoints = collection.mutable.ArrayBuffer.empty[MeshPoint]
  // A cache of where we've put indicies as we use them.
  val indexCache = collection.mutable.Map.empty[parser.ParsedFaceIndex, Int]
  def emptyPoint = MeshPoint(Vec3(0f,0f,0f), Vec3(0f,0f,0f),  Vec2(0f,0f))
  def bakedPointFor(parseIndex: parser.ParsedFaceIndex): MeshPoint = {
    import parse.{vertices, normals, textureCoords}
    val vertex = vertices(parseIndex.vertix - 1)
    val normal = 
      if(parseIndex.normal == 0) Vec3(0.0f,0.0f,0.0f)
      else normals(parseIndex.normal - 1)
    val texture = 
      if (parseIndex.texture == 0) Vec2(0f,0f)
      else textureCoords(parseIndex.texture - 1)
    MeshPoint(vertex,normal,texture)
  }
  def bakedIndexFor(parseIndex: parser.ParsedFaceIndex): Int =
    indexCache.get(parseIndex).getOrElse {
      val idx = bakedPoints.size
      bakedPoints += bakedPointFor(parseIndex)
      indexCache.put(parseIndex, idx)
      idx
    }
  // We translate all faces to triangles.
  def bakeFace(parsed: parser.ParsedFace): Seq[MeshTriangle] =
    parsed match {
      case parser.ParsedFace.Triangle(a,b,c) =>
        Seq(MeshTriangle(bakedIndexFor(a), bakedIndexFor(b), bakedIndexFor(c)))
      case parser.ParsedFace.Quad(a,b,c,d) =>
        Seq(
          MeshTriangle(bakedIndexFor(a), bakedIndexFor(b), bakedIndexFor(c)),
          MeshTriangle(bakedIndexFor(c), bakedIndexFor(d), bakedIndexFor(a)))
    }
  // Update all the groups to make uses of our new vertex+extra encoding of data.  
  val bakedGroups =
    for (group <- parse.groups) 
    yield MeshGroup(
      group.faces.flatMap(bakeFace).toArray,
      // TODO - bake materials
      noMaterial)
  BakedMesh(bakedPoints.toArray, bakedGroups.toArray)
}

// Helper for calculateCentroid.
private val oneThird = 1.0f / 3.0f
/** Calculate the centroid (assuming equal weight on points). */  
def calculateCentroid(points: Array[MeshPoint], faces: Array[MeshTriangle]): Vec3[Float] = {
    // Algorithm
    // C = Centroid <vector>, A = (area of a face * 2)
    // R = face centroid = average of vertices making the face <vector>
    // C = [sum of all (A*R)] / [sum of all R]
    var sumAreaTimesFaceCentroidX = 0f 
    var sumAreaTimesFaceCentroidY = 0f
    var sumAreaTimesFaceCentroidZ = 9f
    var sumArea = 0.0f
      for (face <- faces) {
        val MeshTriangle(fone, ftwo, fthree) = face
        val one = points(fone).vert
        val two = points(ftwo).vert
        val three = points(fthree).vert
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
