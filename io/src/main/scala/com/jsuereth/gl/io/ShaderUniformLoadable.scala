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

package com.jsuereth.gl.io


import org.lwjgl.system.MemoryStack

/** Denotes that a variable can be loaded into an OpenGL shader as a uniform. */
trait ShaderUniformLoadable[T] {
  def loadUniform(location: Int, value: T) given ShaderLoadingEnvironment: Unit
}

/** The environment we make use of when loading a shader. */
trait ShaderLoadingEnvironment {
  /** Memory we can use for this shader. */
  def stack: MemoryStack
  /** A mechanism we can that lets us dynamically select an available texture slot. */
  def textures: ActiveTextures

  def push(): Unit = textures.push()
  def pop(): Unit = textures.pop()
}

import org.lwjgl.opengl.GL20.{glUniform1f, glUniform1i}
object ShaderUniformLoadable {
  /** Helper to pull the stack off the shader loading environment. */
  def stack given ShaderLoadingEnvironment: MemoryStack = the[ShaderLoadingEnvironment].stack
  /** Helper to grab active texutres off the shader loading environment. */
  def textures given ShaderLoadingEnvironment: ActiveTextures = the[ShaderLoadingEnvironment].textures


  delegate for ShaderUniformLoadable[Float] {
    def loadUniform(location: Int, value: Float) given ShaderLoadingEnvironment: Unit =
      glUniform1f(location, value)
  }

  delegate for ShaderUniformLoadable[Int] {
    def loadUniform(location: Int, value: Int) given ShaderLoadingEnvironment: Unit =
      glUniform1i(location, value)
  }
  import com.jsuereth.gl.math._
  delegate Matrix3x3FloatLoader for ShaderUniformLoadable[Matrix3x3[Float]] {
    import org.lwjgl.opengl.GL20.glUniformMatrix3fv
    def loadUniform(location: Int, value: Matrix3x3[Float]) given ShaderLoadingEnvironment: Unit = {
      val buf: java.nio.FloatBuffer = {
        // Here we're allocating a stack-buffer.  We need to ensure we're in a context that has a GL-transition stack.
        val buf = stack.callocFloat(9)
        buf.clear()
        buf.put(value.values)
        buf.flip()
        buf
      }
      glUniformMatrix3fv(location, false, buf)
    }
  }
  delegate Matrix4x4FloatUniformLoader for ShaderUniformLoadable[Matrix4x4[Float]] {
    import org.lwjgl.system.MemoryStack
    import org.lwjgl.opengl.GL20.glUniformMatrix4fv
    def loadUniform(location: Int, value: Matrix4x4[Float]) given ShaderLoadingEnvironment: Unit = {
      val buf: java.nio.FloatBuffer = {
        // Here we're allocating a stack-buffer.  We need to ensure we're in a context that has a GL-transition stack.
        val buf = stack.calloc(sizeOf[Matrix4x4[Float]])
        buf.clear()
        buf.load(value)
        buf.flip()
        buf.asFloatBuffer
      }
      glUniformMatrix4fv(location, false, buf)
    }
  }
  // TODO - report bugs about naming
  delegate loadVec2Float for ShaderUniformLoadable[Vec2[Float]] {
    import org.lwjgl.opengl.GL20.glUniform2f
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: Int, value: Vec2[Float]) given ShaderLoadingEnvironment: Unit =
      glUniform2f(location, value.x, value.y)
  }
  delegate loadVec2Int for ShaderUniformLoadable[Vec2[Int]] {
    import org.lwjgl.opengl.GL20.glUniform2i
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: Int, value: Vec2[Int]) given ShaderLoadingEnvironment: Unit =
      glUniform2i(location, value.x, value.y)
  }
  delegate loadVec3Float for ShaderUniformLoadable[Vec3[Float]] {
    import org.lwjgl.opengl.GL20.glUniform3f
    import org.lwjgl.opengl.GL20.glUniform4f
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: Int, value: Vec3[Float]) given ShaderLoadingEnvironment: Unit =
      glUniform3f(location, value.x, value.y, value.z)
  }
  delegate loadVec3Int for ShaderUniformLoadable[Vec3[Int]] {
    import org.lwjgl.opengl.GL20.glUniform3i
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: Int, value: Vec3[Int]) given ShaderLoadingEnvironment: Unit =
    glUniform3i(location, value.x, value.y, value.z)
  }
  delegate loadVec4Float for ShaderUniformLoadable[Vec4[Float]] {
    import org.lwjgl.opengl.GL20.glUniform4f
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: Int, value: Vec4[Float]) given ShaderLoadingEnvironment: Unit =
      glUniform4f(location, value.x, value.y, value.z, value.w)
  }
  delegate loadVec4Int for ShaderUniformLoadable[Vec4[Int]] {
    import org.lwjgl.opengl.GL20.glUniform4i
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: Int, value: Vec4[Int]) given ShaderLoadingEnvironment: Unit =
    glUniform4i(location, value.x, value.y, value.z, value.w)
  }

  // Now opaque types, i.e. textures + buffers.
  import com.jsuereth.gl.texture._
  delegate for ShaderUniformLoadable[Texture2D] {
    def loadUniform(location: Int, value: Texture2D) given ShaderLoadingEnvironment: Unit = {
      // TODO - figure out how to free acquired textures in a nice way
      val id = textures.acquire()
      // TOOD - is this needed?
      // glEnable(GL_TEXTURE_2D)
      activate(id)
      value.bind()
      // Bind the texture
      // Tell OpenGL which id we used.
      loadTextureUniform(location, id)
    }
  }
  import deriving._
  inline def uniformSize[T]: Int = 
    inline compiletime.erasedValue[T] match {
      case _: (head *: tail) => uniformSize[head] + uniformSize[tail]
      case _: Unit => 0
      case _ => delegate match {
        case m: Mirror.ProductOf[T] => uniformSize[m.MirroredElemTypes]
        case primitive: ShaderUniformLoadable[T] => 1
        case _ => compiletime.error("Type is not a valid uniform value!")
      }
    }

  /** Derives uniform loading for struct-like case classes.   These MUST have statically known sized. */
  inline def derived[T] given (m: Mirror.ProductOf[T]): ShaderUniformLoadable[T] =
    new ShaderUniformLoadable[T] {
      def loadUniform(location: Int, value: T) given ShaderLoadingEnvironment: Unit =
        loadStructAtIdx[m.MirroredElemTypes](location, 0, value.asInstanceOf[Product])
    }

  /** Inline helper to load a value into a location, performing the implicit lookup INLINE. */
  inline def loadOne[T](location: Int, value: T) given ShaderLoadingEnvironment: Unit =
     delegate match {
        case loader: ShaderUniformLoadable[T] =>
          loader.loadUniform(location, value)
        case _ =>
          compiletime.error("Could not find a uniform serializer for all types!")
     }

  /** Peels off a level of case-class property and writes it to a uniform, before continuing to call itself on the next uniform. */
  inline def loadStructAtIdx[RemainingElems](location: Int, idx: Int, value: Product) given ShaderLoadingEnvironment: Unit = {
    inline compiletime.erasedValue[RemainingElems] match {
      case _: Unit => () // Base case, no elements left.
      case _: Tuple1[head] => loadOne[head](location, value.productElement(idx).asInstanceOf)
      case _: (head *: tail) =>
        // Peel off and load this element.
        loadOne[head](location, value.productElement(idx).asInstanceOf)
        // Chain the rest of the elements, allowing for nested structures.
        val nextLoc = location + uniformSize[head]
        loadStructAtIdx[tail](nextLoc, idx+1, value)
    }
  }
}

