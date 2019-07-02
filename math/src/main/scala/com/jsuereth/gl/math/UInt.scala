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
package math

// TODO - move this somewhere it'll work.
opaque type UInt = Int

def (x: UInt) toLong: Long = x & 0xffffffffL
def (x: UInt) + (y: UInt): UInt = x + y
def (x: UInt) - (y: UInt): UInt = x - y
def (x: UInt) * (y: UInt): UInt = x * y
def (x: UInt) / (y: UInt): UInt = (x.toLong / y.toLong).toInt
def (x: UInt) < (y: UInt): Boolean = x.toLong < y.toLong
def (x: UInt) <= (y: UInt): Boolean = x.toLong <= y.toLong
def (x: UInt) > (y: UInt): Boolean = x.toLong > y.toLong
def (x: UInt) >= (y: UInt): Boolean = x.toLong >= y.toLong
def (x: UInt) == (y: UInt): Boolean = x == y
def (x: UInt) != (y: UInt): Boolean = x != y

object UInt {
    def apply(x: Int): UInt = x
    def apply(x: Long): UInt = x.toInt
}