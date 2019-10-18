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
import com.jsuereth.gl.math.{given, _}
import com.jsuereth.gl.texture.Texture2D
import com.jsuereth.gl.io.ShaderUniformLoadable

case class WorldData(light: Vec3[Float], eye: Vec3[Float], view: Matrix4[Float], projection: Matrix4[Float]) derives ShaderUniformLoadable

object CartoonShader extends DslShaderProgram
  // Hack to get structures to work.  OpenGL spec + reality may not line up on how to look up structure ids.  Need to rethink
  // ShaderUniformLoadable class...
  val world = Uniform[WorldData]()
  val modelMatrix = Uniform[Matrix4[Float]]()
  val materialShininess = Uniform[Float]()
  val materialKd = Uniform[Float]()
  val materialKs = Uniform[Float]()
  // Textures in lighting model.
  val materialKdTexture = Uniform[Texture2D]()

  val (vertexShaderCode, fragmentShaderCode) = defineShaders {
    val inPosition = Input[Vec3[Float]](location=0)
    val inNormal = Input[Vec3[Float]](location=1)
    val texPosition = Input[Vec2[Float]](location=2)
    // TODO - convert to matrix3...
    val worldPos = (modelMatrix() * Vec4(inPosition, 1)).xyz
    val worldNormal = (modelMatrix() * Vec4(inNormal, 0)).xyz
    // Note: We have to do this dance to feed this into fragment shader.
    val texCoord: Vec2[Float] = texPosition
    glPosition(world().projection * world().view * modelMatrix() * Vec4(inPosition, 1))
    // Fragment shader
    fragmentShader {
      val L = (world().light - worldPos).normalize
      val N = worldNormal.normalize
      val lambertian = Math.max(L.dot(N), 0.0f).toFloat
      val V = (world().eye - worldPos).normalize
      val H = (L + V).normalize
      val diffuse = materialKd() * lambertian
      val specular =
        if lambertian > 0.0f then materialKs() * Math.pow(Math.max(0, H.dot(N)).toFloat, materialShininess()).toFloat
        else 0.0f
      //Black color if dot product is smaller than 0.3
      //else keep the same colors
      val edgeDetection = if V.dot(worldNormal) > 0.3f then 1f else 0.7f
      val texColor = materialKdTexture().texture(texCoord).xyz
      // TODO - add ambient light?
      val light = (((texColor*diffuse) + (texColor*specular)) * edgeDetection)
      Output("color", 0, Vec4(light, 1f))
    }
  }


object SimpleShader extends DslShaderProgram
  val (vertexShaderCode, fragmentShaderCode) = defineShaders {
      val position = Input[Vec3[Float]](location = 0)
      glPosition(Vec4(position, 1.0f))
      fragmentShader {
          Output("color", 0, Vec4(0.0f, 0.5f, 0.5f, 1.0f))
      }
  }
