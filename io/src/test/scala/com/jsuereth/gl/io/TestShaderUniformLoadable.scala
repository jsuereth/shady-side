package com.jsuereth.gl
package io

import org.junit.Test
import org.junit.Assert._
import org.lwjgl.opengl.GL11.{GL_FLOAT, GL_INT, GL_SHORT, GL_BYTE,GL_DOUBLE}

case class UniformExamplePod(one: math.Vec2[Int], two: math.Vec3[Float])
case class UniformExamplePod2(one: math.Vec2[Int], two: math.Vec3[Float], three: math.Vec2[Float], four: math.Vec2[Float])
case class UniformExamplePod3(one: math.Vec2[Int], two: UniformExamplePod)

class TestUniformSize {
    import ShaderUniformLoadable.uniformSize

     @Test def sizeOfPrimitive(): Unit = {
        assertEquals(1, uniformSize[math.Vec2[Int]])
        assertEquals(1, uniformSize[math.Vec3[Float]])
        assertEquals(1, uniformSize[math.Matrix4x4[Float]])
    }
    @Test def sizeOfStruct(): Unit = {
        assertEquals(2, uniformSize[UniformExamplePod])
        // Recursive size test.
        assertEquals(3, uniformSize[UniformExamplePod3])
        assertEquals(4, uniformSize[UniformExamplePod2])
    }

    // The test here is if we compile.  Calling OpenGL at this point will cause an error, since we have no gl context.
    @Test def deriveUnfiromCompiles(): Unit = {
        delegate testPod for ShaderUniformLoadable[UniformExamplePod] = ShaderUniformLoadable.derived[UniformExamplePod]
        // Test nested derivation.
        delegate testPod2 for ShaderUniformLoadable[UniformExamplePod3] = ShaderUniformLoadable.derived[UniformExamplePod3]
    }
}