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

import com.jsuereth.gl.shaders._
import com.jsuereth.gl.math.{given _, _}
import com.jsuereth.gl.texture.Texture2D
import com.jsuereth.gl.io.ShaderUniformLoadable

case class WorldData(light: Vec3[Float], eye: Vec3[Float], view: Matrix4[Float], projection: Matrix4[Float]) derives ShaderUniformLoadable


object SimpleLightShader extends DslShaderProgram {
  // Hack to get structures to work.  OpenGL spec + reality may not line up on how to look up structure ids.  Need to rethink
  // ShaderUniformLoadable class...
  val world = Uniform[WorldData]()
  val modelMatrix = Uniform[Matrix4[Float]]()
  val materialShininess = Uniform[Float]()
  val materialKd = Uniform[Vec3[Float]]()
  val materialKs = Uniform[Vec3[Float]]()
  // Textures in lighting model.
  val materialKdTexture = Uniform[Texture2D]()

  val (vertexShaderCode, fragmentShaderCode) = defineShaders {
    // TODO - we want Input => MeshPoint directly.
    val inPosition = Input[Vec3[Float]](location=0)
    val inNormal = Input[Vec3[Float]](location=1)
    val texPosition = Input[Vec2[Float]](location=2)

    // Note: We have to do this dance to feed this into fragment shader.
    val texCoord: Vec2[Float] = texPosition
    glPosition(world().projection * world().view * modelMatrix() * Vec4(inPosition, 1))
    // Fragment shader
    fragmentShader {
      val diffuse = materialKdTexture().texture(texCoord).xyz
      Output("color", 0, Vec4(diffuse, 1f))
    }
  }
}
