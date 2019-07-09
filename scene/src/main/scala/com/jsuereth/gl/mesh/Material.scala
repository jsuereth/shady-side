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
package mesh

import math._
import io._

/** Defines the baseline material we will parse out of .obj/.mtl files.  We will need to manipualte this before sending into OpenGL. */
case class RawMaterial(name: String, base: BaseMaterial, textures: MaterialTextures)

/** Definition of material that only includes color definitions, no texture maps. */
case class BaseMaterial(
    /** Ambient Color */
    ka: Vec3[Float] = Vec3(0f,0f,0f),
    /** Diffuse Color */
    kd: Vec3[Float] = Vec3(0f,0f,0f),
    /** Specular Color */
    ks: Vec3[Float] = Vec3(0f,0f,0f),
    /** Specular exponent (shininess) */
    ns: Float = 1f
    // Ignoring transparency and illumination modes.
    // TODO - texture maps?
) derives BufferLoadable /*, ShaderUniformLoadable */ 


// Defines all possible material textures we could parse.
case class MaterialTextures(
    ka: Option[TextureReference] = None,
    kd: Option[TextureReference] = None,
    ks: Option[TextureReference] = None,
    ns: Option[TextureReference] = None,
    d: Option[TextureReference] = None,
    bump: Option[TextureReference] = None
)


/** A referernce to a texture, including texture options. */
case class TextureReference(filename: String, options: TextureOptions)
/** All the options .mtl allows for textures.  We ignore almost all of this. */
case class TextureOptions(
    horizontalBlend: Boolean = true,
    verticalBlend: Boolean = true,
    /** Boost mip-map sharpness. */
    boost: Option[Float] = None,
    /** Modify texture map values: base_value = brightness, gain_value = contrast. */
    modifyMap: (Float,Float) = (0f, 1f),
    originOffset: Vec3[Float] = Vec3(0f,0f,0f),
    scale: Vec3[Float] = Vec3(1f,1f,1f),
    turbulence: Vec3[Float] = Vec3(0f,0f,0f),
    resolution: Option[(Int,Int)] = None,
    clamp: Boolean = false,
    bumpMultiplier: Float = 1f
    // TODO image channel for bump-maps.

)