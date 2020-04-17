import org.junit.Test
import org.junit.Assert._

import com.jsuereth.gl.shaders._
import com.jsuereth.gl.math._
import com.jsuereth.gl.io._
import com.jsuereth.gl.test.assertCleanEquals

/** This is our target syntax. */
import com.jsuereth.gl.math.{given _, _}

case class Material(kd: Float, ks: Float, color: Vec3[Float]) derives ShaderUniformLoadable
object ExampleStructShader extends DslShaderProgram {
  val material = Uniform[Material]()
  val (vertexShaderCode, fragmentShaderCode) = defineShaders {
    val inPosition = Input[Vec3[Float]](location=0)
    glPosition(Vec4(inPosition, 1f))
    fragmentShader {
      Output("color", 0, Vec4(material().color, 1f))
    }
  }
}

class TestStructs {
  @Test def extractVertexShader(): Unit = {
    assertCleanEquals(
"""#version 300 es

precision highp float;
precision highp int;

layout (location = 0) in vec3 inPosition;
void main() {
  gl_Position = vec4(inPosition,1.0);
}""", ExampleStructShader.vertexShaderCode)
  }
  @Test def extractPixelShader(): Unit = {
    assertCleanEquals(
"""#version 300 es

precision highp float;
precision highp int;

struct Material {
  float kd;
  float ks;
  vec3 color;
};
layout (location = 0) out vec4 color;
uniform Material material;
void main() {
  color = vec4((material).color,1.0);
}""", ExampleStructShader.fragmentShaderCode)
    // We should find the first value of the structure here.
    // TODO - figure out how to test parrtial structure usage.
    assertEquals("material", ExampleStructShader.material.name)
  }
}