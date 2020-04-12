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

/** A context by which we can look up resources to load. */
trait MeshResourceLookup {
  def read[A](location: String)(f: java.io.InputStream => A): A
}

final class ClassloaderResourceLookup(
  loader: ClassLoader = classOf[MeshResourceLookup].getClassLoader) 
  extends MeshResourceLookup {
  override def read[A](location: String)(f: java.io.InputStream => A): A = {
    val in = loader.getResourceAsStream(location)
    if (in == null) {
      throw new RuntimeException(s"Failed to find resource: $location")
    }
    try f(in)
    finally in.close()
  }
}

class MeshLoader(resources: MeshResourceLookup = ClassloaderResourceLookup()) {
  def loadObjects(location: String): Map[String,BakedMesh] = {
    val parse = resources.read(location)(parser.ObjFileParser.parse)
    // TODO - we need to synthesize the material lib reference location
    // to be relative to the current location.
    // This is likely true for TEXTURES as well, but we have not
    // loaded/resolved them yet and WE SHOULD, or relativize their resoruce
    // location.
    def relativeTo(ref: String, orig: String): String = {
      val path = java.nio.file.Paths.get(orig)
      val sib = path.resolveSibling(ref)
      // Hack so we don't use windows file layout for classloaders.
      (0 until sib.getNameCount).map(sib.getName).mkString("/")
    }

    val materialLookups = 
      (for {
        lib <- parse.materialLibRefs
        (name, mtl) <- resources.read(relativeTo(lib, location))(parser.MtlFileParser.parse)
      } yield name -> mtl).toMap
    for ((name, raw) <- parse.objects)
    yield name -> bake(raw, materialLookups)
  }
}