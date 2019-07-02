# ShadySide - OpenGL hackery in Scala 3.0

This project aims to run Scala 3.0 through the ringer via producing 
an OpenGL DSL on top of LWJGL.  The API should allow the following:

- GLSL shaders defined fully within Scala, staged and commpiled on GPU.
- "Fast" vector/matrix library for canonical graphics/gpu applications to
  pair w/ the GLSL shader API.
- Easy support for stack-allocation hook of LWJGL
- No attempt, at ALL, to be a "game engine".
- Modest attempt at a linear algebra library, to be hacked on
  when it seems fun.

This is a direct re-write of a personal project from 2013 where I was first learning OpenGL in Scala.  As such, it retains many mistakes
and poor assumptions.  

This project is split into several components:

## Linear Algebra
A linear algebra library that provides missing types required in OpenGL shader language, specifically:

* UInt
* Vec2[T]
* Vec3[T]
* Vec4[T]
* Matrix3x3[T]
* Matrix4x4[T]

In addition to baseline types, this library also provides some abstractions around types to be usable for numbers, specfically:

* Trigonometry[T]
* Rootable[T]

An other types useful in implementing scene graphs/engines.

## OpenGL I/O
A library that helps get data into/out of graphics memory.  This consists of a few core pieces:

* BufferLoadable[T] - load data into java.nio.ByteBuffers
* VertexArrayObject[T] - load POD-type case classes into VAO.  Can load raw vertices, or vertex+index arrays.

## OpenGL Shader DSL
A library that allows specifying OpenGL shaders completely in scala.  Relies on Linear Algebra types, and the I/O library for sending
data into/out of graphics memory.

## Scene Graph
A library to help simplify setting up a Scene in OpenGL and rendering it.   This is used for the example application.

# TODOs

Here's a set of TODOs for this project.  These are just the ones we find interesting enough to list out.  There are probably more, if there's one you're interested in working on, list it out and work on it.

## Linear Algebra library
- [ ] Add Matrix2x2
- [ ] Add (optimal) Matrix multiplication
- [ ] Create fuzzy equals method (floating point friendly)
- [ ] Optimise matrix inverses
- [ ] Create a legitimate set of tests
- [ ] Derive "Show" typeclass for pretty-printing
- [ ] Matrix +/- operations.

## OpenGL I/O library
- [ ] Encode Textures and "opaque" types
- [ ] Write tests for buffer loading

## Shader DSL
- [ ] use compiler error messages instead of exceptions.
- [ ] support helper/nested methods
- [ ] support opaque type: Sampler2D
- [ ] support unsigned ints
- [ ] Add check for outside references that are not uniforms
- [ ] Figure out a way to enforce vertex input in GLSL aligns with vertex buffer input from CPU.

## Scene Graph
- [ ] Material support
- [ ] non-static scenes?

## Example
- [ ] load/use a texture
- [ ] add a post-process shader (anti-aliasing?)

## Bugs to be reported to Dotty

- [ ] Investigate using float literals and open types leading to stack-overflow
- [ ] Investigate having open-type + constrained type in overloaded method causing
  issues in type inference/method discovery
- [ ] Investigate anonymous delegate for multiple fully qualified type constructor
  having name collisions.
- [ ] Report inconsistency of companion-object vs. free-floating delegates/givens.  


# Community Guidelines

See [CONTRIBUTING.md] and [CODE_OF_CONDUCT.md]

# License

Copyright 2019 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

See LICENSE.md for more info.