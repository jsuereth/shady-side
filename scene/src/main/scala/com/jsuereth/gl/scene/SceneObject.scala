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

import mesh.Mesh3d
import math._
/** 
 * Represents an object in the scene. 
 * TODO - Define how to interact w/ objects, and how we render them.
 *
 * TODO - define as enum?
 */
trait SceneObject {
  /** The mesh representing this object. */
  def mesh: Mesh3d
  /** The current position of hte object. */
  def pos: Vec3[Float]
  /** The orientation of the object.  We use a quaternion to save space on rotational axes. */
  def orientation: Quaternion[Float]
  /** Scale of the mesh. */
  def scale: Vec3[Float]
  /** Returns the current model-matrix we can use to render this scene object. */
  def modelMatrix: Matrix4[Float] =
    Matrix4.translate[Float](pos.x, pos.y, pos.z) * orientation.toMatrix * Matrix4.scale(scale.x, scale.y, scale.z)
}
/**
 * A static, unomving object within the scene.
 */
case class StaticSceneObject(mesh: Mesh3d, pos: Vec3[Float], orientation: Quaternion[Float] = Quaternion.identity, scale: Vec3[Float]) extends SceneObject