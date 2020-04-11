package com.jsuereth.gl
package io
package testbufferlodable

import math._

import org.junit.Test
import org.junit.Assert._
import java.nio.ByteBuffer

case class ExamplePod(x: Vec3[Float], y: Vec2[Int], z: Vec2[Boolean]) derives BufferLoadable
class TestBufferLoadable {

    @Test def derivedLoadableWorks(): Unit = {
        val buf = ByteBuffer.allocate(sizeOf[ExamplePod])
        buf.load(ExamplePod(Vec3(1f,2f,3f), Vec2(4,5), Vec2(true, false)))
        buf.flip
        assertEquals(1f, buf.getFloat, 0.000001f)
        assertEquals(2f, buf.getFloat, 0.000001f)
        assertEquals(3f, buf.getFloat, 0.000001f)
        assertEquals(4, buf.getInt)
        assertEquals(5, buf.getInt)
        assertEquals(1.toByte, buf.get)
        assertEquals(0.toByte, buf.get)
    }
    @Test def primitiveWorks(): Unit = {
        val buf = ByteBuffer.allocate(12)
        buf.load(1f)
        buf.load(2)
        buf.load(false)
        buf.load(2.toByte)
        buf.flip
        assertEquals(1f, buf.getFloat, 0.00001f)
        assertEquals(2, buf.getInt)
        assertEquals(0.toByte, buf.get)
        assertEquals(2.toByte, buf.get)
    }
}