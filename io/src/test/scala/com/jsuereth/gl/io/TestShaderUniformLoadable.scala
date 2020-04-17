package com.jsuereth.gl
package io

import org.junit.Test
import org.junit.Assert._
import org.lwjgl.opengl.GL11.{GL_FLOAT, GL_INT, GL_SHORT, GL_BYTE,GL_DOUBLE}

case class UniformExamplePod(one: math.Vec2[Int], two: math.Vec3[Float])
case class UniformExamplePod2(one: math.Vec2[Int], two: math.Vec3[Float], three: math.Vec2[Float], four: math.Vec2[Float])
case class UniformExamplePod3(one: math.Vec2[Int], two: UniformExamplePod)

class TestUniformSize {
    // The test here is if we compile.  Calling OpenGL at this point will cause an error, since we have no gl context.
    @Test def deriveUnfiromCompiles(): Unit = {
        given testPod as ShaderUniformLoadable[UniformExamplePod] = ShaderUniformLoadable.derived[UniformExamplePod]
        // Test nested derivation.
        given testPod2 as ShaderUniformLoadable[UniformExamplePod3] = ShaderUniformLoadable.derived[UniformExamplePod3]
    }
}