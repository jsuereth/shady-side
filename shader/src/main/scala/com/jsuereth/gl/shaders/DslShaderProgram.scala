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

package com.jsuereth.gl.shaders

import com.jsuereth.gl.math._
import com.jsuereth.gl.texture.{Texture2D}

import scala.quoted._
import scala.quoted.autolift._
import scala.annotation.compileTimeOnly

import com.jsuereth.gl.io.ShaderUniformLoadable
import org.lwjgl.system.MemoryStack

/** this object contains our helper macros for this DSL. */
object DslShaderProgram {
    def defineShadersImpl[T](f: Expr[T])(given ctx: QuoteContext): Expr[(String,String)] = {
        import ctx.tasty._
        val helpers = codegen.Convertors[ctx.tasty.type](ctx.tasty)
        val (vert,frag) = helpers.convert(f.unseal)
        // TODO - we need to detect cross/references between shaders and lift into input/output variables.
        '{Tuple2(${Expr(vert.toProgramString)}, ${Expr(frag.toProgramString)})}
    }

    def valNameImpl(given ctx: QuoteContext): Expr[String] = {
      import ctx.tasty._
      // TODO - Detect a structure and look up its first uniform member as the location, i.e. make its name be
      // that member, which is hacky, but an "ok" workaround.
      ctx.tasty.rootContext.owner match {
         case IsValDefSymbol(self) => Expr(self.name)
         // TODO - real error message!
         case _ => throw new RuntimeException(s"Uniform() must be directly assigned to a val")
      }
    }
    inline def valName: String = ${valNameImpl}
    def valNameOrFirstStructNameImpl[T](given ctx: QuoteContext, tpe: Type[T]): Expr[String] = {
      import ctx.tasty._
      val helpers = codegen.Convertors[ctx.tasty.type](ctx.tasty)
      if (helpers.isStructType(tpe.unseal.tpe)) { 
        '{${valNameImpl} + "." + ${Expr(helpers.firstStructMemberName(tpe.unseal.tpe).get)}} 
      } else valNameImpl
    }
    inline def valOrStructName[T]: String = ${valNameOrFirstStructNameImpl[T]}

    def testStructDefImpl[T](given ctx: QuoteContext, tpe: Type[T]): Expr[String] = {
      import ctx.tasty._
      val helpers = codegen.Convertors[ctx.tasty.type](ctx.tasty)
      Expr(helpers.toStructDefinition(tpe.unseal.tpe).map(_.toProgramString).toString)
    }
    inline def testStructDef[T] = ${testStructDefImpl[T]}
}
abstract class DslShaderProgram extends BasicShaderProgram {

  // TODO - we'd also like to implicit-match eitehr ShaderUniformLoadable *or* OpaqueGlslType.
  inline def Uniform[T : ShaderUniformLoadable](): Uniform[T] = MyUniform[T](DslShaderProgram.valOrStructName[T])

  // API for defining shaders...

  inline def defineShaders[T](f: => T): (String,String) = ${DslShaderProgram.defineShadersImpl('f)}
  inline def fragmentShader[T](f: => T): Unit = f


  // COMPILE-TIME ONLY METHODS.  TODO - move this into some kind of API file...
  // Allow DslShaders to access uniform values, but force this call to be within DSL.
  @compileTimeOnly("Can only access a uniform within a Shader.")
  def (c: Uniform[T]) apply[T](): T = ???
  @compileTimeOnly("Can only define input within a shader.")
  def Input[T](location: Int): T = ???
  @compileTimeOnly("Can only define output within a shader.")
  def Output[T](name: String, location: Int, value: T): Unit = ???
  @compileTimeOnly("Can only define glPosition within a shader.")
  def glPosition[T](vec: Vec4[T]): Unit = ???
  /** Samples a texture at a coordinate, pulling the color back. */
  @compileTimeOnly("Textures can only be sampled within a fragment shader.")
  def (c: Texture2D) texture(coord: Vec2[Float]): Vec4[Float] = ???
}