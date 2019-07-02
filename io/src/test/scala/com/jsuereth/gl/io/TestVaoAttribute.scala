package com.jsuereth.gl
package io

import org.junit.Test
import org.junit.Assert._
import org.lwjgl.opengl.GL11.{GL_FLOAT, GL_INT, GL_SHORT, GL_BYTE,GL_DOUBLE}

case class ExamplePod(one: math.Vec2[Int], two: math.Vec3[Float])
case class ExamplePod2(one: math.Vec2[Int], two: math.Vec3[Float], three: math.Vec2[Float], four: math.Vec2[Float])

class TestSize {
    @Test def sizeOfPrimitive(): Unit = {
        assertEquals(2, sizeOf[Short])
        assertEquals(4, sizeOf[Float])
        assertEquals(4, sizeOf[Int])
        assertEquals(8, sizeOf[Double])
        assertEquals(8, sizeOf[Long])
    }
    @Test def sizeOfGlTypes(): Unit = {
        assertEquals(3, sizeOf[math.Vec3[Boolean]])
        assertEquals(12, sizeOf[math.Vec3[Float]])
        assertEquals(36, sizeOf[math.Matrix3x3[Int]])
        assertEquals(16, sizeOf[math.Vec2[Double]])
        assertEquals(8*16, sizeOf[math.Matrix4x4[Long]])
        assertEquals(16, sizeOf[math.Vec4[Int]])
    }
    @Test def sizeofPod(): Unit = {
        assertEquals(20, sizeOf[ExamplePod])
        assertEquals(36, sizeOf[ExamplePod2])
    }
}

class TestGlType {
    @Test def calculateTypeOfPrimative(): Unit = {
        assertEquals(GL_FLOAT, glType[Float])
        assertEquals(GL_INT, glType[Int])
        assertEquals(GL_SHORT, glType[Short])
        assertEquals(GL_BYTE, glType[Byte])
        assertEquals(GL_BYTE, glType[Boolean])
        assertEquals(GL_DOUBLE, glType[Double])
    }
    @Test def calculateTypeOfGlSlTypes(): Unit = {
        assertEquals(GL_FLOAT, glType[math.Vec3[Float]])
        assertEquals(GL_INT, glType[math.Matrix4[Int]])
    }
}

class TestGlSize {
    @Test def calculateGlSizeOfPrimative(): Unit = {
        assertEquals(1, glSize[Float])
        assertEquals(1, glSize[Int])
    }
    @Test def calculateGlSizeOfGlSlTypes(): Unit = {
        assertEquals(2, glSize[math.Vec2[Boolean]])
        assertEquals(3, glSize[math.Vec3[Float]])
        assertEquals(4, glSize[math.Vec4[Int]])
        assertEquals(9, glSize[math.Matrix3[Float]])
        assertEquals(16, glSize[math.Matrix4[Int]])
    }
}

class TestVaoAttribute {
    @Test def calculateVaoAttributes(): Unit = {
        val Array(one,two) = vaoAttributes[ExamplePod]
        assertEquals(one, VaoAttribute(0, 2, GL_INT, 20, 0))
        assertEquals(two, VaoAttribute(1, 3, GL_FLOAT, 20, 8))
    }
    @Test def calculateVaoAttributes2(): Unit = {
        val Array(one,two,three,four) = vaoAttributes[ExamplePod2]
        assertEquals(one,   VaoAttribute(0, 2, GL_INT, 36, 0))
        assertEquals(two,   VaoAttribute(1, 3, GL_FLOAT, 36, 8))
        assertEquals(three, VaoAttribute(2, 2, GL_FLOAT, 36, 20))
        assertEquals(four,  VaoAttribute(3, 2, GL_FLOAT, 36, 28))
    }
}