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

/** The results of parsing an obj file. */
class ObjParsedMesh(override val vertices: Seq[Vec3[Float]],
                    override val normals: Seq[Vec3[Float]],
                    override val textureCoords: Seq[Vec2[Float]],
                    override val faces: Seq[Face]) extends Mesh3d {
  override def toString: String = s"ObjFileMesh(faces=${faces.size})"
}
object ObjFileParser {
  def parse(in: File): Map[String,Mesh3d] =
    parse(java.io.FileInputStream(in))
  def parse(in: InputStream): Map[String,Mesh3d] = {
    val tmp = ObjFileParser()
    tmp.parse(in)
  }
}
/** A parser for .obj files.  Only really handles the ones I've used, not robust. 
 *  Also, ignores material files.
 */
class ObjFileParser {
  private var currentObjectParser: Option[(String, ObjMeshParser)] = None
  private var meshes = collection.mutable.Map[String, ObjParsedMesh]()

  def parse(in: File): Map[String,ObjParsedMesh] = {
    parse(java.io.FileInputStream(in))
  }

  def parse(in: InputStream): Map[String,ObjParsedMesh] = {
    try readStream(in)
    finally in.close()
    meshes.toMap
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
      case ObjLine("usemtl", mtl +: Nil) => System.err.println("Ignoring material: " + mtl) // TODO - figure out what to do w/ materials.
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
  private var faces = collection.mutable.ArrayBuffer.empty[Face]
  /** Read a line of input for a named object. */
  def readLine(line: String): Unit =
    line match {
      case ObjLine("g", group +: Nil) => System.err.println(s"Ignoring group: $group")
      case ObjLine("s", config +: Nil) => System.err.println(s"Ignoring smooth shading config: $config")
      case ObjLine("usemtl", mtl +: Nil) => System.err.println("Ignoring material: " + mtl)
      case ObjLine("v", P3f(x,y,z))  => vertices.append(Vec3(x, y, z))  // verticies
      case ObjLine("vt", P2f(u,v))   => textureCoords.append(Vec2(u,v)) // texture coords
      case ObjLine("vt", P3f(u,v,_))   => textureCoords.append(Vec2(u,v)) // ignore 3d texture coords
      case ObjLine("vn", P3f(x,y,z)) => normals.append(Vec3(x,y,z).normalize)    // vertex normals
      case ObjLine("f", F(face))     => faces.append(face)
      // TODO - support polylines ("l")
      case msg => System.err.println(s"Could not read line in object: $msg") // TODO - better errors.
    }

  def obj: ObjParsedMesh = {
    // TODO - Clean up imported objects.
    // Convert all quad faces into triangles
    val fixedFaces: Seq[TriangleFace] = faces.flatMap {
      case x: TriangleFace => Seq(x)
      case QuadFace(one,two,three,four) => Seq(TriangleFace(one,two,three), TriangleFace(three, four, one))
    }.toSeq
    // Generate normals if needed, update faces.
    val faces2 = if (normals.isEmpty) generateNormals(fixedFaces) else fixedFaces
    ObjParsedMesh(vertices.toSeq, normals.toSeq, textureCoords.toSeq, faces2.toSeq)
  }

  private def generateNormals(faces: Seq[TriangleFace]): Seq[TriangleFace] = {
    for (v <- vertices) normals += Vec3(0f,0f,0f)
    for (TriangleFace(one,two,three) <- faces) {
      val A = vertices(one.vertix-1)
      val B = vertices(two.vertix-1)
      val C = vertices(three.vertix-1)
      val p = (B-A).cross(C-A)
      normals(one.vertix-1) += p
      normals(two.vertix-1) += p
      normals(three.vertix-1) += p
    }
    for (i <- 0 until normals.size) {
      normals(i) = normals(i).normalize
    }
    for {
      TriangleFace(one,two,three) <- faces
    } yield TriangleFace(one.copy(normal=one.vertix), two.copy(normal=two.vertix), three.copy(normal=three.vertix))
  }
}

private[mesh] object F {
  def unapply(face: Seq[String]): Option[Face] =
    face match {
      case Seq(FI(one), FI(two), FI(three)) => Some(TriangleFace(one,two,three))
      case Seq(FI(one), FI(two), FI(three), FI(four)) => Some(QuadFace(one,two,three,four))
      case _ => None
   }
}

private[mesh] object FI {
  def unapply(face: String): Option[FaceIndex] =
    face split "/" match {
      case Array(It(one)) => Some(FaceIndex(one, 0, 0))
      case Array(It(one), It(two)) => Some(FaceIndex(one, two, 0))
      case Array(It(one), It(two), It(three)) =>  Some(FaceIndex(one, two, three))
      case Array(It(one), "", It(three)) =>  Some(FaceIndex(one, 0, three))
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