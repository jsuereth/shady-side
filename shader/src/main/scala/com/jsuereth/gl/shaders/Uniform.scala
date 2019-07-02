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
package shaders

import org.lwjgl.system.MemoryStack

/** A marker trait for how we pass data in/out of an open-gl shader. */
trait Uniform[T] {
    /** The name we use for the uniform. */
    def name: String
    /** Writes  a value into a shader program. */
    def :=(x: T) given MemoryStack: Unit
}