package com.jsuereth.gl.math

import org.junit.Test
import org.junit.Assert._

// TODO - test this w/ scalacheck.
class TestVec3 {

  @Test
  def boolOperations(): Unit = {
    assertEquals(true, Vec3(true,false,false).x)
    assertEquals(true, Vec3(false,true,false).y)
    assertEquals(true, Vec3(false,false,true).z)
  }

  @Test
  def intOperations(): Unit = {
    assertEquals("length of vectors",3, Vec3(1, 1, 1).lengthSquared)
    assertEquals("add vectors", Vec3(-3, 1, 1), Vec3(-1, 0, 1) + Vec3(-2, 1, 0))
    assertEquals("Subtract vectors", Vec3(2, 1, 0), Vec3(3, 5, -1) - Vec3(1, 4, -1))
    assertEquals("Multiply vectors", Vec3(8, 16, 4), Vec3[Int](4, 8, 2) * 2)
    assertEquals("Dot product", 3, Vec3(1,3,-5) dot Vec3(4,-2,-1))
    assertEquals("Cross product", Vec3(-3,6,-3), Vec3(2,3,4) cross Vec3(5,6,7))
    assertEquals("negate", Vec3(-1,-2,-3), Vec3(1,2,3).negate)
    //assertEquals("normalize", Vec3(1,0,0), Vec3(8, 0, 0).normalize)
    // TODO - as/conversions.
  }

  @Test
  def floatOperations(): Unit = {
    val err = 0.0001f
    assertEquals("length of vectors",1.0f, Vec3(1.0f, 0.0f, 0.0f).lengthSquared, err)
    assertEquals("add vectors", Vec3(1.0f, 1.0f, 1.0f), Vec3(0.2f, 1.0f, 0.6f) + Vec3(0.8f, 0.0f, 0.4f))
    // TODO - delta comparison.
    assertEquals("Subtract vectors", Vec3(1.0f, 1.0f, 1.0f), Vec3(1.2f, 1.35f, 0.6f) - Vec3(0.2f, 0.35f, -0.4f))
    assertEquals("Multiply vectors", Vec3(2.0f, 4.0f, 1.0f), Vec3[Float](4.0f, 8.0f, 2.0f) * 0.5f)
    // TODO - dot
    // TODO - cross
    // TODO - negate
    assertEquals("normalize", Vec3(1.0f,0.0f,0.0f), Vec3(8.0f, 0.0f, 0.0f).normalize)
    // TODO - as/conversions.
  }

  @Test
  def doubleOperations(): Unit = {
    assertEquals("length of vectors",1.0, Vec3(1.0, 0.0, 0.0).lengthSquared, 0.01)
    assertEquals("add vectors", Vec3(1.0, 1.0, 1.0), Vec3(0.2, 1.0, 0.6) + Vec3(0.8, 0.0, 0.4))
    assertEquals("Subtract vectors", Vec3(1.0, 1.0, 1.0), Vec3(1.2, 1.3, 0.6) - Vec3(0.2, 0.3, -0.4))
    assertEquals("Multiply vectors", Vec3(2.0, 4.0, 1.0), Vec3[Double](4.0, 8.0, 2.0) * 0.5)
    // TODO - dot
    // TODO - cross
    // TODO - negate
    // TODO - normalize
    // TODO - as/conversions.
  }

  // TODO - Boolean vec.
}
