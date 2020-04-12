/*
 * Copyright 2020 Google LLC
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

val noMaterial =
  RawMaterial("none", BaseMaterial(), MaterialTextures())

/** This converts the raw OBJ file parse into something we can render. */
def bake(parse: parser.ParsedObj, materialLookups: Map[String, RawMaterial]): BakedMesh = {
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
      group.materialRef.flatMap(materialLookups.get).getOrElse(noMaterial))
  BakedMesh(bakedPoints.toArray, bakedGroups.toArray)
}
