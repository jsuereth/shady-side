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

package com.jsuereth.gl.shaders

import org.lwjgl.opengl.{
    GL11,
    GL20
}
import com.jsuereth.gl.io.{
  ShaderUniformLoadable,
  ShaderLoadingEnvironment,
  UniformLocation,
  shapeToLocation
}
import org.lwjgl.system.MemoryStack

enum Shader(val id: Int) {
    case Vertex extends Shader(GL20.GL_VERTEX_SHADER)
    case Fragment extends Shader(GL20.GL_FRAGMENT_SHADER)
}

class ShaderException(msg: String, source: String, cause: Throwable) extends Exception(s"$msg\n\n$source", cause) {
    def this(msg: String, source: String) = this(msg, source: String, null)
}

/** Compiles a shader and returns the id of the shader. */
def compileShader(shaderType: Shader, source: String): Int = {
    val shader = GL20.glCreateShader(shaderType.id)
    try {
        if(shader == 0) throw ShaderException("Unable to construct shader!", source)
        GL20.glShaderSource(shader, source)
        GL20.glCompileShader(shader)
        if(GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw ShaderException(GL20.glGetShaderInfoLog(shader, 2000), source)
        }
    } catch {
        case e: Exception =>
        GL20.glDeleteShader(shader)
        throw e
    }
    shader
}
/** Creates a shader program and linkes in a set of shaders together. */
def linkShaders(compiledShaders: Int*): Int = {
    val program = GL20.glCreateProgram()
    try {
      for (shader <- compiledShaders) GL20.glAttachShader(program, shader) 
    
      GL20.glLinkProgram(program)
      if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
        throw ShaderException(GL20.glGetProgramInfoLog(program, 2000), "")
      }
      // VALIDATION!
      GL20.glValidateProgram(program)
      if(GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
        throw ShaderException(GL20.glGetProgramInfoLog(program, 2000), "")
      }
    } catch {
      // Make sure we clean up if we run into issues.
      case t => GL20.glDeleteProgram(program); throw t
    }
    program
}

abstract class BasicShaderProgram {
    /** Returns the vertex shader for this program. */
    def vertexShaderCode: String
    /** Returns the fragment shader for this program. */
    def fragmentShaderCode: String

    private var programId: Int = 0
    /** Loas the configured shader into the graphics card. */
    def load() : Unit = {
      unload()  
      // TODO - remember shader ids for unloading?
      programId = linkShaders(compileShader(Shader.Vertex, vertexShaderCode),
                              compileShader(Shader.Fragment, fragmentShaderCode))
    }
    /** Removes this shader from OpenGL/Graphics card memory. */
    def unload(): Unit = if (programId != 0) {
        // TODO - unlink shaders?
        // GL20.glDetachShader(program, shader)
        // GL20.glDeleteShader(shader)
        GL20.glDeleteProgram(programId)
        programId = 0
    }
    /** Configures the OpenGL pipeline to use this program. */
    def bind(): Unit = GL20.glUseProgram(programId)
    /** Configures OpenGL pipeline to not use any shader. */
    def unbind(): Unit = GL20.glUseProgram(0)


   /** Our version of uniform which binds to the shader being compiled by this program. */
   protected class MyUniform[T : ShaderUniformLoadable](override val name: String) extends Uniform[T] {
     var location: Option[UniformLocation] = None
     // TODO - does this need to be threadsafe?
     def :=(value: T)(using ShaderLoadingEnvironment): Unit = {
       if (location.isEmpty) {         
         location = Some(
           shapeToLocation(programId, name,
                           summon[ShaderUniformLoadable[T]].shape))
       }
       summon[ShaderUniformLoadable[T]].loadUniform(location.get, value)
     }
     
   }

    def makeUniform[T : ShaderUniformLoadable](name: String): Uniform[T] = MyUniform[T](name)

    // Temporary for debugging purposes only.
    def debugUniform(name: String): Int = GL20.glGetUniformLocation(programId, name)
}