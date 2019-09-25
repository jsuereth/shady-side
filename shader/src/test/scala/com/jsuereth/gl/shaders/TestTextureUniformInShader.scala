package com.jsuereth.gl
package shaders
package testtexture

import org.junit.Test
import org.junit.Assert._

import com.jsuereth.gl.shaders._
import com.jsuereth.gl.math._
import com.jsuereth.gl.test.assertCleanEquals
import com.jsuereth.gl.texture.Texture2D

/** This is our target syntax. */
// Attempt at cel-shading
import com.jsuereth.gl.math.{given, _}

object TextureShader extends DslShaderProgram {

  // User for pixel shader / lighting model
  val diffuseTexture = Uniform[Texture2D]()

  val (vertexShaderCode, fragmentShaderCode) = defineShaders {
    val inPosition = Input[Vec3[Float]](location=0)
    glPosition(Vec4(inPosition, 1))
    // Fragment shader
    fragmentShader {
      Output("color", 0, diffuseTexture().texture(Vec2(0f,0f)))
    }
  }
}

class TestTextureShaders {
  @Test def createsCorrectVertexShader(): Unit = {
    assertCleanEquals(
"""#version 300 es

precision highp float;
precision highp int;

layout (location = 0) in vec3 inPosition;
void main() {
  gl_Position = vec4(inPosition,1.0);
}""", TextureShader.vertexShaderCode)
  }    
@Test def createsCorrectFragmentShader(): Unit = {
  assertCleanEquals(
"""#version 300 es

precision highp float;
precision highp int;

layout (location = 0) out vec4 color;
uniform sampler2D diffuseTexture;
void main() {
  color = texture(diffuseTexture,vec2(0.0,0.0));
}""", TextureShader.fragmentShaderCode)
  }
}