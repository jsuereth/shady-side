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

package com.jsuereth.gl.math

import scala.math.Ordering

// This file defines helper methods to replace the use of java.lang.Math.

/** Returns the maximum value of two options. */
def max[T: Ordering](lhs: T, rhs: T): T = summon[Ordering[T]].max(lhs,rhs)
/** Returns the minimum value of two options. */
def min[T: Ordering](lhs: T, rhs: T): T = summon[Ordering[T]].min(lhs,rhs)