import org.junit.Test
import org.junit.Assert._

import com.jsuereth.gl.shaders._
import com.jsuereth.gl.math._
import com.jsuereth.gl.io._

/** This is our target syntax. */
// Attempt at cel-shading
import com.jsuereth.gl.math._
import delegate com.jsuereth.gl.math._
object ExampleCartoonShader extends DslShaderProgram {
  // Used for vertex shader
  val modelMatrix = Uniform[Matrix4[Float]]()
  val viewMatrix = Uniform[Matrix4[Float]]()
  val projectionMatrix = Uniform[Matrix4[Float]]()

  // User for pixel shader / lighting model
  val lightPosition = Uniform[Vec3[Float]]()
  val eyePosition = Uniform[Vec3[Float]]()
  val materialShininess = Uniform[Int]()
  val materialKd = Uniform[Float]()
  val materialKs = Uniform[Float]()

  val (vertexShaderCode, fragmentShaderCode) = defineShaders {
    val inPosition = Input[Vec3[Float]](location=0)
    val inNormal = Input[Vec3[Float]](location=1)
    // TODO - convert to matrix3...
    val worldPos = (modelMatrix() * Vec4(inPosition, 1)).xyz
    val worldNormal = (modelMatrix() * Vec4(inNormal, 0)).xyz

    glPosition(projectionMatrix() * viewMatrix() * modelMatrix() * Vec4(inPosition, 1))
    // Fragment shader
    fragmentShader {
      val L = (lightPosition() - worldPos).normalize
      val V = (eyePosition() - worldPos).normalize
      val H = (L + V).normalize
      val diffuse = materialKd() * Math.max(0, L.dot(worldNormal))
      val specular =
        if (L.dot(worldNormal) > 0.0f) materialKs() * Math.pow(Math.max(0, H.dot(worldNormal)).toFloat, materialShininess()).toFloat
        else 0.0f
      //Black color if dot product is smaller than 0.3
      //else keep the same colors
      val edgeDetection = if (V.dot(worldNormal) > 0.3f) 1f else 0f
      val light = edgeDetection * (diffuse + specular)
      Output("color", 0, Vec4(light, light, light, 1f))
    }
  }
}


class Test1 {
  @Test def extractCartoonVertexShader(): Unit = {
    assertEquals(
"""#version 300 es

precision highp float;
precision highp int;

out vec3 worldPos;
out vec3 worldNormal;
layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec3 inNormal;
uniform mat4 modelMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
void main() {
  worldPos = ((modelMatrix * vec4(inPosition,1.0))).xyz;
  worldNormal = ((modelMatrix * vec4(inNormal,0.0))).xyz;
  gl_Position = (((projectionMatrix * viewMatrix) * modelMatrix) * vec4(inPosition,1.0));
}""", ExampleCartoonShader.vertexShaderCode)
  }
  @Test def extractCartoonFragmentShader(): Unit = {
    assertEquals(
"""#version 300 es

precision highp float;
precision highp int;

in vec3 worldPos;
in vec3 worldNormal;
uniform vec3 lightPosition;
uniform vec3 eyePosition;
uniform float materialKd;
uniform float materialKs;
uniform int materialShininess;
layout (location = 0) out vec4 color;
void main() {
  vec3 L = normalize((lightPosition - worldPos));
  vec3 V = normalize((eyePosition - worldPos));
  vec3 H = normalize((L + V));
  float diffuse = (materialKd * max(0.0,dot(L,worldNormal)));
  float specular = ((dot(L,worldNormal) > 0.0)) ? ((materialKs * pow(max(0.0,dot(H,worldNormal)),materialShininess))) : (0.0);
  float edgeDetection = ((dot(V,worldNormal) > 0.3)) ? (1.0) : (0.0);
  float light = (edgeDetection * (diffuse + specular));
  color = vec4(light,light,light,1.0);
}""", ExampleCartoonShader.fragmentShaderCode)

    assertEquals("modelMatrix", ExampleCartoonShader.modelMatrix.name)
    assertEquals("lightPosition", ExampleCartoonShader.lightPosition.name)
  }
}