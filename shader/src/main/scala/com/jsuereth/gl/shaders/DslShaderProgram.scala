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
import scala.annotation.compileTimeOnly

import com.jsuereth.gl.io.ShaderUniformLoadable
import org.lwjgl.system.MemoryStack

/** this object contains our helper macros for this DSL. */
object DslShaderProgram {
    def defineShadersImpl[T](f: Expr[T])(using ctx: QuoteContext): Expr[(String,String)] = {
        import ctx.tasty._
        val helpers = codegen.Convertors[ctx.tasty.type](ctx.tasty)
        val (vert,frag) = helpers.convert(f.unseal)
        // TODO - we need to detect cross/references between shaders and lift into input/output variables.
        '{Tuple2(${Expr(vert.toProgramString)}, ${Expr(frag.toProgramString)})}
    }

    def valNameImpl(using ctx: QuoteContext): Expr[String] = {
      import ctx.tasty._
      // QUick hack to look up the owner hierarhcy for a legit name...
      def findNonMacroSymbol(o: Symbol): Symbol =
         if (o.name == "macro") findNonMacroSymbol(o.owner)
         else o
      // TODO - Detect a structure and look up its first uniform member as the location, i.e. make its name be
      // that member, which is hacky, but an "ok" workaround.
      findNonMacroSymbol(ctx.tasty.rootContext.owner) match {
         case o: Symbol if o.isValDef => Expr(o.name)
         case _ => 
           ctx.error("Uniform() must be directly assigned to a val")
           Expr("")
      }
    }
    inline def valName: String = ${valNameImpl}
    def valNameOrFirstStructNameImpl[T](using ctx: QuoteContext, tpe: Type[T]): Expr[String] = {
      import ctx.tasty._
      val helpers = codegen.Convertors[ctx.tasty.type](ctx.tasty)
      if (helpers.isStructType(tpe.unseal.tpe)) { 
        '{${valNameImpl} + "." + ${Expr(helpers.firstStructMemberName(tpe.unseal.tpe).get)}} 
      } else valNameImpl
    }
    inline def valOrStructName[T]: String = ${valNameOrFirstStructNameImpl[T]}

    def testStructDefImpl[T](using ctx: QuoteContext, tpe: Type[T]): Expr[String] = {
      import ctx.tasty._
      val helpers = codegen.Convertors[ctx.tasty.type](ctx.tasty)
      Expr(helpers.toStructDefinition(tpe.unseal.tpe).map(_.toProgramString).toString)
    }
    inline def testStructDef[T] = ${testStructDefImpl[T]}
}
abstract class DslShaderProgram extends BasicShaderProgram {

  // TODO - we'd also like to implicit-match either ShaderUniformLoadable *or* OpaqueGlslType.
  inline def Uniform[T : ShaderUniformLoadable](): Uniform[T] =
    MyUniform[T](DslShaderProgram.valOrStructName[T])

  // API for defining shaders...

  inline def defineShaders[T](inline f: => T): (String, String) = ${DslShaderProgram.defineShadersImpl('f)}
  inline def fragmentShader[T](inline f: => T): Unit = f


  // COMPILE-TIME ONLY METHODS.  TODO - move this into some kind of API file...
  // Allow DslShaders to access uniform values, but force this call to be within DSL.
  @compileTimeOnly("Can only access a uniform within a Shader.")
  inline def [T](c: Uniform[T]) apply(): T = ???
  @compileTimeOnly("Can only define input within a shader.")
  inline def Input[T](location: Int): T = ???
  @compileTimeOnly("Can only define output within a shader.")
  inline def Output[T](name: String, location: Int, value: T): Unit = ???
  @compileTimeOnly("Can only define glPosition within a shader.")
  inline def glPosition[T](vec: Vec4[T]): Unit = ???
  /** Samples a texture at a coordinate, pulling the color back. */
  @compileTimeOnly("Textures can only be sampled within a fragment shader.")
  inline def (c: Texture2D) texture(coord: Vec2[Float]): Vec4[Float] = ???
}