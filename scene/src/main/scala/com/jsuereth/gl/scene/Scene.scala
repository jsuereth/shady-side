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
package scene

import math.{Vec3, Quaternion}
import collection.mutable.ArrayBuffer
import mesh.Mesh3d
/** 
 * Defines the base of a renderable scene. 
 * 
 * A scene can be more complicated than this base trait, but here are the absolute minimal characteristics.
 */
trait Scene
    /** The camera to use for this scene. */
    def camera: Camera
    // TODO - figure out what kind of light abstraction we want to show off different shaders...
    /** Position of all lights in the scene (if multiple). */
    def lights: Iterator[Vec3[Float]]
    /** Objects to render in the scene. */
    def objectsInRenderOrder: Iterator[SceneObject]
    // TODO - def updateSimulation, etc....

/** A simple static scene that contains a few objects. */
final class SimpleStaticScene(
    override val camera: Camera, 
    val light: Vec3[Float],
    val objects: Seq[SceneObject]
) extends Scene
    override def lights: Iterator[Vec3[Float]] = Iterator(light)
    override def objectsInRenderOrder: Iterator[SceneObject] = objects.iterator

/** Scene builder syntax. */
class SimpleStaticSceneBuilder {
  private var c = Camera()
  private var l = Vec3(0f, 10f, 0f)
  private val os = ArrayBuffer[SceneObject]()
  
  def light(location: Vec3[Float]): this.type =
      this.l = location
      this

  def camera(c: Camera): this.type =
      this.c = c
      this

  def add(obj: SceneObject): this.type =
      this.os += obj
      this
  def add(mesh: Mesh3d): ObjBuilder = ObjBuilder(mesh)

  class ObjBuilder(mesh: Mesh3d)
      private[this] var p = Vec3(0f,0f,0f)
      private[this] var o = Quaternion.identity[Float]
      private[this] var s = Vec3(1f,1f,1f)
      def pos(pos: Vec3[Float]): this.type =
          this.p = pos
          this
      def orientation(o: Quaternion[Float]): this.type =
          this.o = o
          this
      def scale(x: Float, y: Float, z: Float): this.type =
          this.s = Vec3(x,y,z)
          this
      def done(): SimpleStaticSceneBuilder =
          add(StaticSceneObject(mesh, p, o, s))

  def done(): Scene = SimpleStaticScene(c, l, os.toSeq)
}