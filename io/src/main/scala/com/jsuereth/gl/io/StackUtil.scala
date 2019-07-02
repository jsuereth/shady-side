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

import org.lwjgl.system.{
  MemoryStack,
  MemoryUtil
}
import java.nio.{
  ByteBuffer,
  FloatBuffer,
  IntBuffer
}

/** Ensures there is a memory stack available for an operation, popping the stack when complete. */
inline def withMemoryStack[A](f: given MemoryStack => A): A = {
  val stack = org.lwjgl.system.MemoryStack.stackPush()
  try f given stack
  finally stack.pop()
}
/** Allocates a byte buffer.  Will allocate on the stack, if small enough, otherwise in off-heap memory. */
inline def withByteBuffer[A](size: Int)(f: ByteBuffer => A) given (stack: MemoryStack): A = 
  if (size < 4096) f(stack.calloc(size))
  else {
    val buf = MemoryUtil.memAlloc(size)
    try f(buf) finally MemoryUtil.memFree(buf)
  }
/** Allocates a float buffer.  Will allocate on the stack, if small enough, otherwise in off-heap memory. */
inline def withFloatBuffer[A](size: Int)(f: FloatBuffer => A) given (stack: MemoryStack): A = 
  if (size < 1024) f(stack.callocFloat(size))
  else {
    val buf = MemoryUtil.memAllocFloat(size)
    try f(buf) finally MemoryUtil.memFree(buf)
  }
/** Allocates an int buffer.  Will allocate on the stack, if small enough, otherwise in off-heap memory. */
inline def withIntBuffer[A](size: Int)(f: IntBuffer => A) given (stack: MemoryStack): A = 
  if (size < 1024) f(stack.callocInt(size))
  else {
    val buf = MemoryUtil.memAllocInt(size)
    try f(buf) finally MemoryUtil.memFree(buf)
  }