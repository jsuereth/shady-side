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
package parser


import java.io.{File, InputStream}
import math.{Vec2,Vec3, given _}


/** A mesh that has been parsed from an ObjFile. */
final case class ParsedObj(
  /** Vertices that have been parsed. */
  vertices: Seq[Vec3[Float]],
  /** All parsed Normals. */
  normals: Seq[Vec3[Float]],
  /** All parsed texture coordinates. */
  textureCoords: Seq[Vec2[Float]],
  /** All parsed obj groups (or one). */
  groups: Seq[ParsedGroup]
)

/** A group that has been parsed from an ObjFile. */
final case class ParsedGroup(
  /** The name of this group. */
  name: String,
  /** The material (by name) to use when rendering this group. */
  materialRef: Option[String],
  /** The faces in this portion of the object. */
  faces: Seq[ParsedFace]) {
    override def toString(): String =
      s"ParsedGroup($name, faceCount: ${faces.length})"
  }

/** The result of parsing a `.obj` file. */
final case class ParsedObjects(
  /** Named objects found in this file and parsed. */
  objects: Map[String, ParsedObj],
  /** Material libraries that should be loaded from this file. */
  materialLibRefs: Seq[String]
)

/** Faces as we can parse them from OBJ files, prior to 'baking' a model. */
enum ParsedFace {
  case Triangle(one: ParsedFaceIndex, two: ParsedFaceIndex, three: ParsedFaceIndex)
  case Quad(one: ParsedFaceIndex, two: ParsedFaceIndex, three: ParsedFaceIndex, four: ParsedFaceIndex)
}
/** Index reference into a Mesh3d for a face-definition. */
final case class ParsedFaceIndex(vertix: Int, texture: Int, normal: Int)


object ObjFileParser {
  def parse(in: File): ParsedObjects =
    parse(java.io.FileInputStream(in))
  def parse(in: InputStream): ParsedObjects = {
    val tmp = ObjFileParser()
    // TODO - wrap in mesh?
    tmp.parse(in)
  }
}
/** A parser for .obj files.  Only really handles the ones I've used, not robust. 
 *  Also, ignores material files.
 */
class ObjFileParser {
  private var currentObjectParser: Option[(String, ObjMeshParser)] = None
  private var meshes = collection.mutable.Map[String, ParsedObj]()
  private var materialLibRefs = collection.mutable.ArrayBuffer[String]()

  def parse(in: File): ParsedObjects = {
    parse(java.io.FileInputStream(in))
  }

  def parse(in: InputStream): ParsedObjects = {
    try readStream(in)
    finally in.close()
    ParsedObjects(meshes.toMap, materialLibRefs.toSeq)
  }

  private def readStream(in: java.io.InputStream): Unit = {
    val buffer = java.io.BufferedReader(java.io.InputStreamReader(in))
    def read(): Unit =
      buffer.readLine match {
        case null => ()
        case line => 
          readLine(line)
          read()
      }
    read()
    cleanSubParser()
  }

  private def cleanSubParser(): Unit =
     currentObjectParser match {
            case Some((name, parser)) => meshes.put(name, parser.obj)
            case _ => None
        }

  private def readLine(line: String): Unit = 
    line match {
      case ObjLine("#", _) | "" => // Ignore comments.
      case ObjLine("mtllib", mtl +: Nil) => materialLibRefs.append(mtl)
      case ObjLine("o", name +: Nil) => 
        cleanSubParser()
        currentObjectParser = Some((name, ObjMeshParser()))
      case msg =>
        currentObjectParser match {
          case Some((_, parser)) => parser.readLine(msg)  
          case None => 
            // Handle unnamed objects
            currentObjectParser = Some(("unnamed", ObjMeshParser()))
            currentObjectParser.get._2.readLine(msg)
        }
    }
}

/** stateful parser we use to look for objects within a .obj file. */
class ObjMeshParser() {
  private var vertices = collection.mutable.ArrayBuffer.empty[Vec3[Float]]
  private var normals = collection.mutable.ArrayBuffer.empty[Vec3[Float]]
  private var textureCoords = collection.mutable.ArrayBuffer.empty[Vec2[Float]]
  private var faces = collection.mutable.ArrayBuffer.empty[ParsedFace]
  private var groups = collection.mutable.ArrayBuffer.empty[ParsedGroup]
  private var currentMaterial: Option[String] = None
  private var currentGroupName: Option[String] = None
  /** Read a line of input for a named object. */
  def readLine(line: String): Unit =
    line match {
      case ObjLine("s", config +: Nil) => System.err.println(s"Ignoring smooth shading config: $config")
      case ObjLine("g", group +: Nil) => pushNextGroup(group)
      // TODO - do we need to record a new object group here?
      case ObjLine("usemtl", mtl +: Nil) => currentMaterial = Some(mtl)
      case ObjLine("v", P3f(x,y,z))  => vertices.append(Vec3(x, y, z))  // verticies
      case ObjLine("vt", P2f(u,v))   => textureCoords.append(Vec2(u,v)) // texture coords
      case ObjLine("vt", P3f(u,v,_))   => textureCoords.append(Vec2(u,v)) // ignore 3d texture coords
      case ObjLine("vn", P3f(x,y,z)) => normals.append(Vec3(x,y,z).normalize)    // vertex normals
      case ObjLine("f", F(face))     => faces.append(face)
      // TODO - support polylines ("l")
      case msg => System.err.println(s"Could not read line in object: $msg") // TODO - better errors.
    }
  /** Pushes the next group into state, and record the previous one... */
  private def pushNextGroup(name: String): Unit = {
    if (!faces.isEmpty) addGroupAndClearData()
    currentGroupName = Some(name)
  }
  private def addGroupAndClearData(): Unit = if (!faces.isEmpty) {
    // Note: if we synthesize normals we need to make triangleshere.
    // we COULD move that logic into baking, perhaps we should.
    val myFaces = makeTriangles(faces.iterator)
    val result = ParsedGroup(
      currentGroupName.getOrElse(s"group-${groups.length}"),
      currentMaterial,
      myFaces
    )
    groups.append(result)
    currentGroupName = None
    //currentMaterial = None
    faces = collection.mutable.ArrayBuffer.empty[ParsedFace]
  }
  /** unifies faces to all be triangles. */
  private def makeTriangles(faces: Iterator[ParsedFace]): Seq[ParsedFace] =
    faces.flatMap {
      case x: ParsedFace.Triangle => Seq(x)
      case ParsedFace.Quad(one,two,three,four) => Seq(ParsedFace.Triangle(one,two,three), ParsedFace.Triangle(three, four, one))
    }.toSeq

  def obj: ParsedObj = {
    addGroupAndClearData()
    // Generate normals if needed, update faces.
    if (normals.isEmpty) generateNormals()
    ParsedObj(
      vertices = vertices.toSeq,
      normals = normals.toSeq,
      textureCoords = textureCoords.toSeq, 
      groups = groups.toSeq)
  }

  // TODO - update this to use the groups...
  private def generateNormalsForGroup(group: ParsedGroup): ParsedGroup = {
    for (ParsedFace.Triangle(one,two,three) <- group.faces) {
          val A = vertices(one.vertix-1)
          val B = vertices(two.vertix-1)
          val C = vertices(three.vertix-1)
          val p = (B-A).cross(C-A)
          normals(one.vertix-1) += p
          normals(two.vertix-1) += p
          normals(three.vertix-1) += p
        }
    val newFaces =        
      for {
        ParsedFace.Triangle(one,two,three) <- group.faces
      } yield ParsedFace.Triangle(one.copy(normal=one.vertix), two.copy(normal=two.vertix), three.copy(normal=three.vertix))
    group.copy(faces = newFaces.toSeq)
  }

  private def generateNormals(): Unit = {
    for (v <- vertices) normals += Vec3(0f,0f,0f)
    groups.mapInPlace(generateNormalsForGroup)
    for (i <- 0 until normals.size) {
      normals(i) = normals(i).normalize
    }
  }
}

private[mesh] object F {
  def unapply(face: Seq[String]): Option[ParsedFace] =
    face match {
      case Seq(FI(one), FI(two), FI(three)) => Some(ParsedFace.Triangle(one,two,three))
      case Seq(FI(one), FI(two), FI(three), FI(four)) => Some(ParsedFace.Quad(one,two,three,four))
      case _ => None
   }
}

private[mesh] object FI {
  def unapply(face: String): Option[ParsedFaceIndex] =
    face split "/" match {
      case Array(It(one)) => Some(ParsedFaceIndex(one, 0, 0))
      case Array(It(one), It(two)) => Some(ParsedFaceIndex(one, two, 0))
      case Array(It(one), It(two), It(three)) =>  Some(ParsedFaceIndex(one, two, three))
      case Array(It(one), "", It(three)) =>  Some(ParsedFaceIndex(one, 0, three))
      case _ => None 
    }
}

private[parser] object P3f {
  def unapply(in: Seq[String]): Option[(Float, Float, Float)] =
    in match {
      case Seq(Fl(x), Fl(y), Fl(z)) => Some((x,y,z))
      case _ => None
    }
}

private[parser] object P2f {
  def unapply(in: Seq[String]): Option[(Float, Float)] =
    in match {
      case Seq(Fl(x), Fl(y)) => Some(x -> y)
      case _ => None
    }
}
private[parser] object ObjLine {
  def unapply(in: String): Option[(String, Seq[String])] = {
    val split = in split "\\s+"
    if(split.isEmpty) None
    else {
      Some(split.head -> split.tail)
    }
  }
    
}

private[parser] object It {
  def unapply(in: String): Option[Int] =
    try Some(in.toInt)
    catch {
      case _: NumberFormatException => None
    }
}
private[parser] object Fl {
  def unapply(in: String): Option[Float] =
    try Some(in.toFloat)
    catch {
      case _: NumberFormatException => None
    }
}