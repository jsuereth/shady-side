package com.jsuereth.gl
package shaders
package testmathtranslation

import org.junit.Test
import org.junit.Assert._

import com.jsuereth.gl.math._

/** This is our target syntax. */
// Attempt at cel-shading
import com.jsuereth.gl.math._
import delegate com.jsuereth.gl.math._
object ExampleShader extends DslShaderProgram {
  // Used for vertex shader
  val lightPosition = Uniform[Vec3[Float]]()
  val eyePosition = Uniform[Vec3[Float]]()

  val (vertexShaderCode, fragmentShaderCode) = defineShaders {
    val inPosition = Input[Vec3[Float]](location=0)
    glPosition(Vec4(inPosition, 1))
    // Fragment shader
    fragmentShader {
      val L = pow(1f, max(min(0f, lightPosition().x), lightPosition().y))
      Output("color", 0, Vec4(L, L, L, 1f))
    }
  }
}

class Test1 {
  @Test def extractVertexShaderProgramString(): Unit = {
    assertEquals(
"""#version 300 es

precision highp float;
precision highp int;

layout (location = 0) in vec3 inPosition;
void main() {
  gl_Position = vec4(inPosition,1.0);
}""", ExampleShader.vertexShaderCode)
  }
  @Test def extractFragmentShaderProgramString(): Unit = {
    assertEquals(
"""#version 300 es

precision highp float;
precision highp int;

uniform vec3 lightPosition;
layout (location = 0) out vec4 color;
void main() {
  float L = pow(1.0,max(min(0.0,(lightPosition).x),(lightPosition).y));
  color = vec4(L,L,L,1.0);
}""", ExampleShader.fragmentShaderCode)
  }
}