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
import org.lwjgl.opengl.{GL20}
import scala.compiletime.summonFrom

// TOOD - we need to do a bit more work here to handle strucutres.
// OpenGL will optimise away structure memebers that are unused.

/** Represents the location-ref in GL memory for a uniform. */
enum UniformLocation {
  /** The location of an individual field. -1 => elided. */
  case FieldLocation(location: Int)
  /** The location of fields within a Struct. */
  case StructLocation(fields: Map[String, UniformLocation])
}

/** The shape of a uniform. */
enum UniformShape {
  /** This uniform is one field/known type to openGL. */
  case Field
  /** This uniform is a structure and needs individual field lookups. */
  case Struct(fields: Map[String, UniformShape])
}

/** Helper method to load locations for a given Shader program/name for a uniform. */
def shapeToLocation(programId: Int, name: String, shape: UniformShape): UniformLocation =
  shape match {
    case UniformShape.Field => 
      UniformLocation.FieldLocation(GL20.glGetUniformLocation(programId, name))
    case UniformShape.Struct(fields) =>
      UniformLocation.StructLocation( for {
        (fname, fshape) <- fields
      } yield fname -> shapeToLocation(programId, s"$name.$fname", fshape))
  }

/** Denotes that a variable can be loaded into an OpenGL shader as a uniform. */
trait ShaderUniformLoadable[T] {
  /** The fields of a structure, used to lookup locations for each member. */
  def shape: UniformShape = UniformShape.Field
  /** Loads the given value into the location in OpenGL. */
  def loadUniform(location: UniformLocation, value: T)(using ShaderLoadingEnvironment): Unit
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
  def stack(using ShaderLoadingEnvironment): MemoryStack = summon[ShaderLoadingEnvironment].stack
  /** Helper to grab active texutres off the shader loading environment. */
  def textures(using ShaderLoadingEnvironment): ActiveTextures = summon[ShaderLoadingEnvironment].textures


  given ShaderUniformLoadable[Float] {
    def loadUniform(location: UniformLocation, value: Float)(using ShaderLoadingEnvironment): Unit =
      glUniform1f(location.asInstanceOf[UniformLocation.FieldLocation].location, value)
  }

  given ShaderUniformLoadable[Int] {
    def loadUniform(location: UniformLocation, value: Int)(using ShaderLoadingEnvironment): Unit =
      glUniform1i(location.asInstanceOf[UniformLocation.FieldLocation].location, value)
  }
  import com.jsuereth.gl.math._
  given Matrix3x3FloatLoader as ShaderUniformLoadable[Matrix3x3[Float]] {
    import org.lwjgl.opengl.GL20.glUniformMatrix3fv
    def loadUniform(location: UniformLocation, value: Matrix3x3[Float])(using ShaderLoadingEnvironment): Unit = {
      val buf: java.nio.FloatBuffer = {
        // Here we're allocating a stack-buffer.  We need to ensure we're in a context that has a GL-transition stack.
        val buf = stack.callocFloat(9)
        buf.clear()
        buf.put(value.values)
        buf.flip()
        buf
      }
      glUniformMatrix3fv(location.asInstanceOf[UniformLocation.FieldLocation].location, false, buf)
    }
  }
  given Matrix4x4FloatUniformLoader as ShaderUniformLoadable[Matrix4x4[Float]] {
    import org.lwjgl.system.MemoryStack
    import org.lwjgl.opengl.GL20.glUniformMatrix4fv
    def loadUniform(location: UniformLocation, value: Matrix4x4[Float])(using ShaderLoadingEnvironment): Unit = {
      val buf: java.nio.FloatBuffer = {
        // Here we're allocating a stack-buffer.  We need to ensure we're in a context that has a GL-transition stack.
        val buf = stack.calloc(sizeOf[Matrix4x4[Float]])
        buf.clear()
        buf.load(value)
        buf.flip()
        buf.asFloatBuffer
      }
      glUniformMatrix4fv(location.asInstanceOf[UniformLocation.FieldLocation].location, false, buf)
    }
  }

  given loadVec2Float as ShaderUniformLoadable[Vec2[Float]] {
    import org.lwjgl.opengl.GL20.glUniform2f
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: UniformLocation, value: Vec2[Float])(using ShaderLoadingEnvironment): Unit =
      glUniform2f(location.asInstanceOf[UniformLocation.FieldLocation].location, value.x, value.y)
  }

  given loadVec2Int as ShaderUniformLoadable[Vec2[Int]] {
    import org.lwjgl.opengl.GL20.glUniform2i
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: UniformLocation, value: Vec2[Int])(using ShaderLoadingEnvironment): Unit =
      glUniform2i(location.asInstanceOf[UniformLocation.FieldLocation].location, value.x, value.y)
  }

  given loadVec3Float as ShaderUniformLoadable[Vec3[Float]] {
    import org.lwjgl.opengl.GL20.glUniform3f
    import org.lwjgl.opengl.GL20.glUniform4f
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: UniformLocation, value: Vec3[Float])(using ShaderLoadingEnvironment): Unit =
      glUniform3f(location.asInstanceOf[UniformLocation.FieldLocation].location, value.x, value.y, value.z)
  }

  given loadVec3Int as ShaderUniformLoadable[Vec3[Int]] {
    import org.lwjgl.opengl.GL20.glUniform3i
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: UniformLocation, value: Vec3[Int])(using ShaderLoadingEnvironment): Unit =
    glUniform3i(location.asInstanceOf[UniformLocation.FieldLocation].location, value.x, value.y, value.z)
  }

  given loadVec4Float as ShaderUniformLoadable[Vec4[Float]] {
    import org.lwjgl.opengl.GL20.glUniform4f
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: UniformLocation, value: Vec4[Float])(using ShaderLoadingEnvironment): Unit =
      glUniform4f(location.asInstanceOf[UniformLocation.FieldLocation].location, value.x, value.y, value.z, value.w)
  }

  given loadVec4Int as ShaderUniformLoadable[Vec4[Int]] {
    import org.lwjgl.opengl.GL20.glUniform4i
    import org.lwjgl.system.MemoryStack
    def loadUniform(location: UniformLocation, value: Vec4[Int])(using ShaderLoadingEnvironment): Unit =
    glUniform4i(location.asInstanceOf[UniformLocation.FieldLocation].location, value.x, value.y, value.z, value.w)
  }

  // Now opaque types, i.e. textures + buffers.
  import com.jsuereth.gl.texture._
  given ShaderUniformLoadable[Texture2D] {
    def loadUniform(location: UniformLocation, value: Texture2D)(using ShaderLoadingEnvironment): Unit = {
      // TODO - figure out how to free acquired textures in a nice way
      val id = textures.acquire()
      // TOOD - is this needed?
      // glEnable(GL_TEXTURE_2D)
      activate(id)
      value.bind()
      // Bind the texture
      // Tell OpenGL which id we used.
      loadTextureUniform(location.asInstanceOf[UniformLocation.FieldLocation].location, id)
    }
  }
  import deriving._

  /** Derives uniform loading for struct-like case classes.   These MUST have statically known sized. */
  inline def derived[T](using m: Mirror.ProductOf[T]): ShaderUniformLoadable[T] =
    new ShaderUniformLoadable[T] {
      // Fix this to be generated...
      override val shape: UniformShape = shapeOf[T]
      def loadUniform(location: UniformLocation, value: T)(using ShaderLoadingEnvironment): Unit =
        location match {
          case u: UniformLocation.StructLocation =>
            loadUniforms[m.MirroredElemTypes, m.MirroredElemLabels](u, value.asInstanceOf[Product], 0)
          case _ => ???
        }
    }

  inline def shapeOf[T]: UniformShape =
    compiletime.summonFrom {
      case m: Mirror.ProductOf[T] => 
        UniformShape.Struct(summonFields[m.MirroredElemTypes, m.MirroredElemLabels])
      case _ => UniformShape.Field
    }

  inline def summonFields[Elems, Labels]: Map[String, UniformShape] =
    inline compiletime.erasedValue[(Elems, Labels)] match {
      case (_: (etype *: etail), _: (label *: ltail)) =>
        val field = compiletime.constValue[label].asInstanceOf[String] -> shapeOf[etype]
        summonFields[etail, ltail] + field
      case (_: Unit, _: Unit) => Map.empty
    }

  inline def loadUniforms[Elems, Labels](location: UniformLocation.StructLocation,
                                         value: Product,
                                         idx: Int)(using ShaderLoadingEnvironment) : Unit =
    inline compiletime.erasedValue[(Elems, Labels)] match {
      case (_: (etype *: etail), _: (label *: ltail)) =>
        loadOne[etype](location.fields(
          compiletime.constValue[label].asInstanceOf[String]),
          value.productElement(idx).asInstanceOf)
        loadUniforms[etail, ltail](location, value, idx+1)
      case (_: Unit, _: Unit) => ()
      case _ => compiletime.error("Unexpected type!")
    }


  /** Inline helper to load a value into a location, performing the implicit lookup INLINE. */
  inline def loadOne[T](location: UniformLocation, value: T)(using ShaderLoadingEnvironment): Unit =
     summonFrom {
        case loader: ShaderUniformLoadable[T] =>
          loader.loadUniform(location, value)
        case _ =>
          compiletime.error("Could not find a uniform serializer for all types!")
     }

  /** Peels off a level of case-class property and writes it to a uniform, before continuing to call itself on the next uniform. */
  inline def loadStructAtIdx[RemainingElems](location: UniformLocation, idx: Int, value: Product)(using ShaderLoadingEnvironment): Unit = {
    inline compiletime.erasedValue[RemainingElems] match {
      case _: Unit => () // Base case, no elements left.
      case _: Tuple1[head] => loadOne[head](location, value.productElement(idx).asInstanceOf)
      case _: (head *: tail) =>
        // Peel off and load this element.
        loadOne[head](location, value.productElement(idx).asInstanceOf)
        // Chain the rest of the elements, allowing for nested structures.
        val nextLoc = location
        loadStructAtIdx[tail](nextLoc, idx+1, value)
    }
  }
}

