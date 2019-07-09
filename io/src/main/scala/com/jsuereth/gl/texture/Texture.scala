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

import io._

import org.lwjgl.opengl.GL11.{
    glEnable,
    GL_FLOAT,
    glBindTexture,
    glGenTextures,
    glDeleteTextures,
    GL_TEXTURE_2D,
    GL_TEXTURE_1D,
    glPixelStorei,
    GL_UNPACK_ALIGNMENT,
    GL_RGBA,
    GL_RGBA8,
    GL_UNSIGNED_BYTE,
    glTexImage2D
}
import org.lwjgl.opengl.GL12.{
    GL_TEXTURE_3D
}
import org.lwjgl.opengl.GL30.{
    glGenerateMipmap
}
/** Represents a texture loaded into OpenGL. */
sealed trait Texture extends io.GLBuffer {
    /** The id of the texture in OpenGL. */
    def id: Int

    override def close(): Unit = glDeleteTextures(id)
}
object Texture {
    // TODO - loading methods for various formats.
    // TODO - mechanism to load into/out of frame buffers...
    import TextureParameter._
    import TextureFilterType.{Linear}
    // Give up on generic texture loading, an,d just do one for PNGs.
    def loadImage(source: java.io.InputStream,
                params: Seq[TextureParameter] = Seq(MinFilter(Linear), MagFilter(Linear)),
                genMipMap: Boolean = true): Texture2D = {
        // Double give-up, we use AWT for now...
        val img = javax.imageio.ImageIO.read(source)
        val pixels = new Array[Int](img.getHeight*img.getWidth)
        img.getRGB(0, 0, img.getWidth, img.getHeight, pixels, 0, img.getWidth)
        // Load RGBA format
        val buf = java.nio.ByteBuffer.allocateDirect(img.getWidth*img.getHeight*4)
        // We invert the y axis for OpenGL.
        for {
            y <- (0 until img.getHeight).reverse
            x <- 0 until img.getWidth
            pixel = pixels(y*img.getWidth + x)
        } {
            // RGBA
            buf.put(((pixel >> 16) & 0xFF).toByte)
            buf.put(((pixel >> 8) & 0xFF).toByte)
            buf.put((pixel & 0xFF).toByte)
            buf.put(((pixel >> 24) & 0xFF).toByte)
        }
        buf.flip()
        val id = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, id)
        import org.lwjgl.opengl.GL11._
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        // TODO - params are not doing the right thing.
        //params.foreach(_.set(GL_TEXTURE_2D))
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, img.getWidth, img.getHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf)
        glBindTexture(GL_TEXTURE_2D, 0);
        Texture2D(id)
    }

    // TODO - is this useful?
    inline def textureType[T]: Int = inline compiletime.erasedValue[T] match {
        case _: Texture1D => GL_TEXTURE_1D
        case _: Texture2D => GL_TEXTURE_2D
        case _: Texture3D => GL_TEXTURE_3D
        case _ => compiletime.error("Not a valid texture type!")
    }
}


/** A reference to a one-dimensional texture. */
case class Texture1D(id: Int) extends Texture {
    def bind(): Unit = glBindTexture(GL_TEXTURE_1D, id)
    def unbind(): Unit = glBindTexture(GL_TEXTURE_1D, 0)
}

/** A reference to a two dimensional texture. */
case class Texture2D(id: Int) extends Texture {
    def bind(): Unit = glBindTexture(GL_TEXTURE_2D, id)
    def unbind(): Unit = glBindTexture(GL_TEXTURE_2D, 0)
}

/** A reference to a three dimensional texture. */
case class Texture3D(id: Int) extends Texture {
    def bind(): Unit = glBindTexture(GL_TEXTURE_3D, id)
    def unbind(): Unit = glBindTexture(GL_TEXTURE_3D, 0)
}

