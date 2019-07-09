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
import math.{Vec2,Vec3}

object MtlFileParser {
  def parse(in: File): Map[String,RawMaterial] =
    parse(java.io.FileInputStream(in))
  def parse(in: InputStream): Map[String,RawMaterial] = {
    val tmp = MtlFileParser()
    tmp.parse(in)
  }
}
class MtlFileParser {
  private var currentParser: Option[(String, MtlParser)] = None
  private var materials = collection.mutable.Map[String, RawMaterial]()


  def parse(in: InputStream): Map[String,RawMaterial] = {
    try readStream(in)
    finally in.close()
    materials.toMap
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
  private def readLine(line: String): Unit = 
    line match {
      case ObjLine("#", _) | "" => // Ignore comments.
      case ObjLine("newmtl", name +: Nil) => 
        cleanSubParser()
        currentParser = Some((name, MtlParser(name)))
      case msg =>
        currentParser match {
          case Some((_, parser)) => parser.readLine(msg)  
          case None => System.err.println(s"Could not read line in mtllib: $msg") // TODO - better errors.
        }
    }
  private def cleanSubParser(): Unit =
    currentParser match {
      case Some((name, parser)) => materials.put(name, parser.mtl)
      case _ => None
    }
}

class MtlParser(name: String) {
    private var base = BaseMaterial()
    private var textures = MaterialTextures()
    def readLine(line: String): Unit = 
        line match {
          case ObjLine("Ka", P3f(x,y,z)) => base = base.copy(ka = Vec3(x,y,z))
          case ObjLine("Kd", P3f(x,y,z)) => base = base.copy(kd = Vec3(x,y,z))
          case ObjLine("Ks", P3f(x,y,z)) => base = base.copy(ks = Vec3(x,y,z))
          case ObjLine("Ns", Seq(Fl(value))) => base = base.copy(ns = value)
          case ObjLine("map_Ka", TextureRef(ref)) => textures = textures.copy(ka = Some(ref))
          case ObjLine("map_Kd", TextureRef(ref)) => textures = textures.copy(kd = Some(ref))
          case ObjLine("map_Ks", TextureRef(ref)) => textures = textures.copy(ks = Some(ref))
          case ObjLine("map_Ns", TextureRef(ref)) => textures = textures.copy(ns = Some(ref))
          case ObjLine("map_d", TextureRef(ref)) => textures = textures.copy(d = Some(ref))
          case ObjLine("map_bump", TextureRef(ref)) => textures = textures.copy(bump = Some(ref))
          case ObjLine("bump", TextureRef(ref)) => textures = textures.copy(bump = Some(ref))
          case msg => System.err.println(s"Could not read line in mtl: $msg") // TODO - better errors.
        }

    def mtl: RawMaterial = RawMaterial(name, base, textures)
}

object TextureRef {
    def unapply(values: Seq[String]): Option[TextureReference] =
      values match {
          case Seq(f) => Some(TextureReference(f, TextureOptions()))
          // TODO - handle texture options.
          case _ => None
      }
}