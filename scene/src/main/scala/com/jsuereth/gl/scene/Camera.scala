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

import math._

/** 
 * Defines a camera for a scene.
 *
 * This class allows simple "camera" operations as if you are a first-person viewer, like "turnRight", "moveForward".
 * It defines itself according to three basic vectors: eye position, up direction and focus position.
 * Using this when rendering requires converting to a ViewMatrix and loading into a shader.
 */
class Camera {
    // TODO - better defaults...
  private var eye = Vec3[Float](0.0f, 0.0f, -10f) // Location of the camera
  private var up = Vec3[Float](0.0f, 1.0f, 0.0f)  // The up vector.
  private var focus = Vec3[Float](0.0f, 0.0f, 0.0f)  // Center of the camera foucs
  /** Returns the current position of the eye. */
  def eyePosition: Vec3[Float] = eye

  // The direction we're looking...
  private def lookDirection = (focus - eye).normalize  
  // The direction to the right.
  private def rightDirection = (lookDirection cross up).normalize
  // TODO - Calculate the right/left direction
  
  def reset(  eye: Vec3[Float] = Vec3(-0.5f, 5.0f, -0.5f), 
               up: Vec3[Float] = Vec3(0.0f, 1.0f, 0.0f), 
            focus: Vec3[Float] = Vec3(0.0f, 0.0f, 0.0f)): Unit =
    this.eye = eye
    this.up = up
    this.focus = focus
  
  def moveRight(amount: Float): Unit =
    val mover = rightDirection * amount
    eye += mover
    focus += mover
    println(this)
  
  def turnRight(amount: Float): Unit =
    val rot = Quaternion.rotation(-amount, up)
    focus = eye + (rot * (focus-eye))
    println(this)
  
  def turnUp(amount: Float): Unit =
    val rot = Quaternion.rotation(amount, rightDirection)
    focus = eye + (rot * (focus-eye))
    println(this)
  
  
  def moveForward(amount: Float): Unit =
    val mover = lookDirection * amount
    focus += mover
    eye += mover
    println(this)
  
  def moveUp(amount: Float): Unit =
    val mover = up * amount
    eye += mover
    focus += mover
    println(this)
  
  def zoom(amount: Float): Unit =
    // TODO - Scale this differently...
    focus = focus + (lookDirection * amount)
  
  def viewMatrix: Matrix4x4[Float] = Matrix4.lookAt(focus, eye, up)
  
  override def toString: String = 
    s"""|Camera Positions
        |- eye:  $eye
        |- focus: $focus
        |- up:  $up""".stripMargin
}