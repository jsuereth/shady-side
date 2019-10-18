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

package com.jsuereth.gl.io

import org.lwjgl.opengl.GL30.{GL_FRAMEBUFFER,glBindFramebuffer,glGenFramebuffers,glDeleteFramebuffers}

/** A frame buffer object that has been allocated in OpenGL. 
 *  You can load/store textures memory in FBOs.
 *  Shader pipelines push data into these.
 */
class FrameBufferObject private (override val id: Int) extends GLBuffer
    def close(): Unit = glDeleteFramebuffers(id)

    // TODO - Allocation of frame-objects on this buffer, specifically Samplers/Textures.

    def bind(): Unit = glBindFramebuffer(GL_FRAMEBUFFER, id)
    def unbind(): Unit = glBindFramebuffer(GL_FRAMEBUFFER, 0)

    // TODO - make public
    // TODO - common error message.
    /** Validates whether or not this frame bufffer is in a consistent/usable state. */
    private def validate(): Unit = withBound {
      import org.lwjgl.opengl.GL30._
      glCheckFramebufferStatus(GL_FRAMEBUFFER) match
        case GL_FRAMEBUFFER_COMPLETE => ()
        case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT => sys.error("Frame buffer incomplete attachment")
        case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER => sys.error("Frame buffer incomplete draw buffer")
        case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT => sys.error("Frame buffer incomplete missing attachment")
        case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER => sys.error("Frame buffer incomplete read buffer")
        case GL_FRAMEBUFFER_UNSUPPORTED => sys.error("Frame buffers are unsupported on this platform.")
        case id => sys.error(s"Unkown issue with framebuffer: $id")
    }

    override def toString: String = s"VBO($id)"
object FrameBufferObject {
    /** Creates a new framebuffer we can use to allocate texture/frame objects. */
    def apply(): FrameBufferObject = new FrameBufferObject(glGenFramebuffers)
}