package com.jsuereth.gl
package mesh
package parser

import org.junit.Test
import org.junit.Assert._
import math._

class TestObjParser {
    def stream(in: String): java.io.InputStream = 
      java.io.ByteArrayInputStream(in.getBytes)

    @Test def parseSimpleMesh(): Unit = {
        val meshes = ObjFileParser.parse(stream("""|
        |o test
        |v 1.0 0.0 1.0
        |v 0.0 0.0 0.0
        |v 1.0 0.0 0.0
        |f 1 2 3
        |""".stripMargin('|')))

        assertEquals(1, meshes.size)
        assertTrue(meshes.contains("test"))
        val mesh: Mesh3d = meshes("test")
        assertEquals(1, mesh.faces.size)
        // Texture coordinates are 0, normal is synthesized
        assertEquals(TriangleFace(FaceIndex(1,0,1), FaceIndex(2,0,2), FaceIndex(3,0,3)), mesh.faces.head)
        assertEquals(3, mesh.vertices.size)
        assertEquals("Parse first point", Vec3(1f,0f,1f), mesh.vertices(0))
        assertEquals("Parse second point", Vec3(0f,0f,0f), mesh.vertices(1))
        assertEquals("Parses third point", Vec3(1f,0f,0f), mesh.vertices(2))
        assertEquals(3, mesh.normals.size)
        assertEquals("Calculate first normal", Vec3(0f,-1f,0f), mesh.normals(0))
        assertEquals("Calculate second normal", Vec3(0f,-1f,0f), mesh.normals(1))
        assertEquals("Calculate third normal", Vec3(0f,-1f,0f), mesh.normals(2))
    }
    @Test def parseMultiObjectMesh(): Unit = {
        val meshes = ObjFileParser.parse(stream("""|
        |o test
        |o test2
        |""".stripMargin('|')))

        assertEquals(2, meshes.size)
        assertTrue(meshes.contains("test"))
        assertTrue(meshes.contains("test2"))
    }
    @Test def useGivenNormals(): Unit = {
        val meshes = ObjFileParser.parse(stream("""|
        |o test
        |v 1.0 0.0 1.0
        |v 0.0 0.0 1.0
        |v 1.0 0.0 1.0
        |vn 0.0 1.0 0.0
        |f  1//1 2//1 3//1
        |""".stripMargin('|')))
        assertEquals(1, meshes.size)
        assertTrue(meshes.contains("test"))
        val mesh: Mesh3d = meshes("test")
        assertEquals(TriangleFace(FaceIndex(1,0,1), FaceIndex(2,0,1), FaceIndex(3,0,1)), mesh.faces.head)
        assertEquals(3, mesh.vertices.size)
        assertEquals("Parse first point", Vec3(1f,0f,1f), mesh.vertices(0))
        assertEquals("Parse second point", Vec3(0f,0f,1f), mesh.vertices(1))
        assertEquals("Parses third point", Vec3(1f,0f,1f), mesh.vertices(2))
        assertEquals(1, mesh.normals.size)
        assertEquals("Parse only normal", Vec3(0f,1f,0f), mesh.normals(0))
    }
}