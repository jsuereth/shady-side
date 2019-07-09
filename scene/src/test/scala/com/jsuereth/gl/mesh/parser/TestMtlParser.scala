package com.jsuereth.gl
package mesh
package parser

import org.junit.Test
import org.junit.Assert._
import math._

class TestMtlParser {
    def stream(in: String): java.io.InputStream = 
      java.io.ByteArrayInputStream(in.getBytes)

    @Test def parseSimpleMaterial(): Unit = {
        val materials = MtlFileParser.parse(stream("""|
        |newmtl default
        |Ns 96.0
        |Ka 0.000000 0.000000 1.000000
        |Kd 0.640000 0.640000 0.640000
        |Ks 0.500000 0.500000 0.500000
        |Ni 1.000000
        |d 1.000000
        |illum 2
        |map_Kd blue-sky.jpg
        |""".stripMargin('|')))

        assertEquals(1, materials.size)
        val mtl = materials("default")
       assertEquals("Parse ambient color", Vec3(0f,0f,1f), mtl.base.ka)
       assertEquals("Parse diffuse color", Vec3(0.64f,0.64f,0.64f), mtl.base.kd)
       assertEquals("Parse specular color", Vec3(0.5f,0.5f,0.5f), mtl.base.ks)
       assertEquals("Parse specular component", 96.0f, mtl.base.ns)
       assertEquals("Parse diffuse texture file", Some("blue-sky.jpg"), mtl.textures.kd.map(_.filename))
       assertEquals("Not Parse specular texture file", None, mtl.textures.ks.map(_.filename))
       assertEquals("Not Parse ambient texture file", None, mtl.textures.ka.map(_.filename))
       assertEquals("Not Parse specular component texture file", None, mtl.textures.ns.map(_.filename))
       assertEquals("Not Parse d texture file", None, mtl.textures.d.map(_.filename))
       assertEquals("Not Parse bump map texture file", None, mtl.textures.bump.map(_.filename))
    }
}