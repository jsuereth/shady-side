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
package texture

import org.lwjgl.opengl.GL11.{
    glTexParameteri,
    GL_TEXTURE_2D,
    GL_CLAMP,
    GL_REPEAT,
    GL_NEAREST,
    GL_LINEAR,
    GL_TEXTURE_WRAP_S,
    GL_TEXTURE_WRAP_T,
    GL_TEXTURE_MAG_FILTER,
    GL_TEXTURE_MIN_FILTER,
    GL_NEAREST_MIPMAP_NEAREST,
    GL_NEAREST_MIPMAP_LINEAR,
    GL_LINEAR_MIPMAP_LINEAR,
    GL_LINEAR_MIPMAP_NEAREST
}
import org.lwjgl.opengl.GL14.{
    GL_MIRRORED_REPEAT
}

/** Controls how textures wrap in OpenGL. */
enum TextureWrapType
    case ClampToEdge
    case MirroredRepeat
    case Repeat 

    def toGL: Int = this match
        case ClampToEdge => GL_CLAMP
        case MirroredRepeat => GL_MIRRORED_REPEAT
        case Repeat => GL_REPEAT

/** Controls how we grab pixel values from textures when we don't have 1:1 mapping from point-to-texture coord. */
enum TextureFilterType
  case Nearest, Linear
  
  def toGL: Int = this match
      case Nearest => GL_NEAREST
      case Linear => GL_LINEAR
/** Defines a parameter for an OpenGL texture, as well as the ability to set that parameter. */
enum TextureParameter {
    case WrapS(tpe: TextureWrapType)
    case WrapT(tpe: TextureWrapType)
    case MagFilter(tpe: TextureFilterType)
    case MinFilter(tpe: TextureFilterType, mipMap: Option[TextureFilterType] = None)

    import TextureFilterType.{Nearest,Linear}
    private def toMipMapTpe(tpe: TextureFilterType, mipMap: TextureFilterType): Int = (tpe, mipMap) match
        case (Nearest, Nearest) => GL_NEAREST_MIPMAP_NEAREST
        case (Nearest, Linear) => GL_NEAREST_MIPMAP_LINEAR
        case (Linear, Linear) => GL_LINEAR_MIPMAP_LINEAR
        case (Linear, Nearest) => GL_LINEAR_MIPMAP_NEAREST

    /** Call the appropriate glTextParameteri() function for this texture parameter. */
    def set(target: Int = GL_TEXTURE_2D): Unit = this match
        case WrapS(tpe) => glTexParameteri(target, GL_TEXTURE_WRAP_S, tpe.toGL)
        case WrapT(tpe) => glTexParameteri(target, GL_TEXTURE_WRAP_T, tpe.toGL)
        case MagFilter(tpe) => glTexParameteri(target, GL_TEXTURE_MAG_FILTER, tpe.toGL)
        case MinFilter(tpe, None) => glTexParameteri(target, GL_TEXTURE_MAG_FILTER, tpe.toGL)
        case MinFilter(tpe, Some(m)) => glTexParameteri(target, GL_TEXTURE_MIN_FILTER, toMipMapTpe(tpe, m))
} 