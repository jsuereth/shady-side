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

import org.lwjgl.system.MemoryStack
import org.lwjgl.opengl.GL30.{GL_FRAMEBUFFER,glBindFramebuffer,glGenFramebuffers}


/** A helper to represent a buffer in OpenGL.  We can load things into this buffer. */
trait GLBuffer extends AutoCloseable {
    /* The ID of the buffer object.  This makes no sense to use unless you know the type of buffer (FBO/VBO). */
    def id: Int
    // all instances should have an inline "with"
    inline def withBound[A](f: => A): A = {
      bind()
      try f
      finally unbind()
    }
    /** Binds the buffer to be used. */
    def bind(): Unit
    /** Unbinds the buffer. */
    def unbind(): Unit
}