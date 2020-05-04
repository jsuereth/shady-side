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
package codegen

import quoted.QuoteContext
import com.jsuereth.gl.math._
import com.jsuereth.gl.texture.{Texture2D}


// Symbols for casts between scala number types that we can ignore in GLSL.
val autoCasts = Set("scala.Float$.float2double","scala.Int$.int2double")

// Arithmetic types we will do operator conversions for.  I.e. we take scala method calls that are operator-like and turn them into GLSL operators.
val arithmeticTypes = Set(
    "scala.Float",
    "scala.Int",
    "scala.Double",
    "com.jsuereth.gl.math.Vec2", 
    "com.jsuereth.gl.math.Vec3",
    "com.jsuereth.gl.math.Vec4",
    "com.jsuereth.gl.math.Matrix4x4",
    "com.jsuereth.gl.math.Matrix3x3")
// TODO - not all of these cross correctly...
val arithmeticOps = Set("+", "-", "*", "/", ">", "<")
val operatorTransforms: Map[String, String] =
    (for {
        tpe <- arithmeticTypes
        op <- arithmeticOps
    } yield s"$tpe.$op" -> op).toMap

// Constructors used to create GLSL types.
val typeApplyMethods = Map[String, String](
    "com.jsuereth.gl.math.Vec4$.apply" -> "vec4",
    "com.jsuereth.gl.math.Vec3$.apply" -> "vec3",
    "com.jsuereth.gl.math.Vec2$.apply" -> "vec2",
    "com.jsuereth.gl.math.Matrix4x4$.apply" -> "mat4",
    "com.jsuereth.gl.math.Matrix3x3$.apply" -> "mat3"
)

// Operations on java.lang.Math that are built-in on GLSL.
val javaMathOperations = Set("max", "min", "pow")
// Operations we've built into our math library that can be translated into GLSL.
val ourMathOperations = Set("max", "min", "pow")

// Instance methods that turn into global methods in GLSL.
val matrixOperations = Set("inverse", "transpose")

trait StmtConvertorEnv {
    def recordUniform(name: String, tpe: String): Unit
    def recordOutput(name: String, tpe: String, location: Int): Unit
    def recordInput(name: String, tpe: String, location: Int): Unit
    def recordStruct(struct: codegen.Declaration.Struct): Unit
}
trait ShaderConvertorEnv extends StmtConvertorEnv {
    def addStatement(stmt: Statement): Unit
    def ast: Ast
}
trait VertexShaderConvertorEnv extends ShaderConvertorEnv {
    def fragmentEnv: ShaderConvertorEnv
}

class DefaultShaderConvertEnv extends ShaderConvertorEnv {
    private var stmts = collection.mutable.ArrayBuffer[codegen.Statement]()
    private var globals = collection.mutable.ArrayBuffer[codegen.Declaration]()
    private var names = Set[String]()
    private var types = Set[String]()
    override def addStatement(stmt: codegen.Statement): Unit = stmts.append(stmt)
    // TODO - record location
    override def recordInput(name: String, tpe: String, location: Int): Unit = 
        if (!names(name)) {
            globals.append(codegen.Declaration.Input(name, tpe, Some(location)))
            names += name
        }
    override def recordUniform(name: String, tpe: String): Unit = 
        if (!names(name)) {
            globals.append(codegen.Declaration.Uniform(name, tpe))
            names += name
        }
    override def recordStruct(struct: codegen.Declaration.Struct): Unit = {
      if (!types(struct.name)) {
        globals.prepend(struct)
        types += struct.name
      }
    }
    override def recordOutput(name: String, tpe: String, location: Int): Unit =
        if (!names(name)) {
          globals.append(codegen.Declaration.Output(name, tpe, Some(location)))
          names += name
        }
    override def ast: codegen.Ast = {
        codegen.Ast(globals.toSeq :+ codegen.Declaration.Method("main", "void", stmts.toSeq))
    }
}

/** Utilities for translating Scala into GLSL. */
class Convertors[R <: tasty.Reflection](val r: R)(using QuoteContext) {
    // TODO - given new API we may be able to use Type/TypeTree directly without
    // hiding it behind tasty.Reflection instance.
    import r._

    def toGlslTypeFromTree(tpe: r.TypeTree): String = toGlslType(tpe.tpe)
    def toGlslType(tpe: r.Type): String = {
        import r._
        // TODO - more principled version of this...
        // TODO - test to ensure this lines up with sizeof, vaoattribute, uniform loader, etc.
        if (tpe <:< typeOf[Vec2[Float]]) "vec2"
        else if (tpe <:< typeOf[Vec2[Int]]) "ivec2"
        else if (tpe <:< typeOf[Vec3[Float]]) "vec3"
        else if (tpe <:< typeOf[Vec3[Int]]) "ivec3"
        else if(tpe <:< typeOf[Vec4[Float]]) "vec4"
        else if(tpe <:< typeOf[Vec4[Int]]) "ivec4"
        else if(tpe <:< typeOf[Matrix4x4[Float]]) "mat4"
        else if(tpe <:< typeOf[Matrix3x3[Float]]) "mat3"
        else if(tpe <:< typeOf[Float]) "float"
        else if(tpe <:< typeOf[Boolean]) "bool"
        else if(tpe <:< typeOf[Int]) "int"
        else if(tpe <:< typeOf[Double]) "double"
        else if(tpe <:< typeOf[Unit]) "void"
        else if(tpe <:< typeOf[Texture2D]) "sampler2D"
        // TODO - a real compiler errors, not a runtime exception.
        else throw new RuntimeException(s"Unknown GLSL type: ${tpe}")
    }

    // TODO - can we make type->glsl type a straight inline method we use in macros?
    def toStructMemberDef(sym: r.Symbol): StructMember = 
      StructMember(sym.name, toGlslType(sym.tree.asInstanceOf[r.ValDef].tpt.tpe))

    /** Converts a given type into a GLSL struct definition. */
    def toStructDefinition(tpe: r.Type): Option[Declaration.Struct] = 
      tpe.classSymbol map { clsSym =>
        new Declaration.Struct(clsSym.name, clsSym.caseFields.map(toStructMemberDef).toSeq)
      }
    /** Checks whether a given type is an GLSL "opaque" type. */  
    def isOpaqueType(tpe: r.Type): Boolean = tpe <:< typeOf[Texture2D]  
    /** Returns true if a given type is a "structure-like" type for the purposes of GLSL. */
    def isStructType(tpe: r.Type): Boolean = tpe.classSymbol match {
      // TODO - more robust definition.  E.g. check if ALL fields have a GLSL type.
      case Some(clsSym) => !clsSym.caseFields.isEmpty && !isOpaqueType(tpe)
      case None => false
    }
    /** Returns the first member name of a structure type, if this is a structure type.   Note: This is a workaround. */
    def firstStructMemberName(tpe: r.Type): Option[String] =
      for {
        struct <- toStructDefinition(tpe)
        member <- struct.members.headOption
      } yield member.name


    /** Extractor for detecting a "fragmentShader {}" block. */
    object FragmentShader {
        def unapply(tree: r.Statement): Option[r.Statement] = tree match {
            case Inlined(Some(Apply(mth, args)), statements, expansion) if mth.symbol.fullName == "com.jsuereth.gl.shaders.DslShaderProgram.fragmentShader" => Some(expansion)
            case _ => None
        }
    }
    object Erased {
      def unapply(tree: r.Statement): Option[r.Tree] =
        tree match {
          case Inlined(Some(erased), stmts, blck) => Some(erased)
          case _ => None
        }
    }

    /** Extractor for val x = Input(...) */
    object Input {
        /** Returns:  name, type, location */
        def unapply(tree: r.Statement): Option[(String, String, Int)] = tree match {
            // defined in object
            case ValDef(sym, tpe, Some(Erased(
              Apply(TypeApply(Ident("Input"), _), List(NamedArg("location", Literal(Constant(location: Int))))))
              )) =>
              Some(sym, toGlslTypeFromTree(tpe), location)
            // Defined in Inline....
            case _ => None
        }
    }
    /** Extractor for:  Output("name", position, value) */
    object Output {
        /** Returns:  name, value  (todo - tpe + location) */
        def unapply(tree: r.Statement): Option[(String, String, r.Statement)] = tree match {
            case Erased(Apply(TypeApply(Ident("Output"), _), List(Literal(Constant(name: String)), position, value))) =>
              Some(name, toGlslType(value.tpe), value)
            case _ => None
        }
    }
    /** Extractor for:  <uniform>() */
    object Uniform {
        /** Returns:  name, type. */
        def unapply(tree: r.Statement): Option[(String, String)] = tree match {
            case Erased(Apply(Apply(TypeApply(Ident("apply"), List(tpe)), List(a @ Ident(ref))), List())) if a.tpe <:< typeOf[Uniform[_]]  => 
              // TODO - handle uniform-style structs.
              if (isStructType(tpe.tpe)) None
              else Some(ref.toString, toGlslType(tpe.tpe))
            case _ => None
        }
    }
    /** Extractor for:  <uniform>(), where we are accessing a struct type. */
    object UniformStruct {
        /** Returns:  name, type and structure definition. */
        def unapply(tree: r.Statement): Option[(String, Declaration.Struct)] = tree match {
            case Erased(Apply(Apply(TypeApply(Ident("apply"), List(tpe)), List(a @ Ident(ref))), List())) if a.tpe <:< typeOf[Uniform[_]] && isStructType(tpe.tpe)  => 
              // TODO - handle uniform-style structs.
              toStructDefinition(tpe.tpe) match {
                case Some(struct) => Some(ref.toString, struct)
                case None => None
              }
            case _ => None
        }
    }
    /** Extractor for: gl_Position =. */
    object GlPosition {
        // TODO - limit glPosition to calling against the appropriate type/symbol...
        def unapply(tree: r.Statement): Option[r.Statement] = tree match {
            // Called by Object
            case Erased(Apply(TypeApply(Ident("glPosition"), _), List(value))) => Some(value)
            // called within Class
            // TODO - update for inlined/erasure.
            case Apply(TypeApply(Select(_, "glPosition"), _), List(value)) => Some(value)
            case _ => None
        }
    }
    /** Detect if scala is converting between int->float->double, etc. where GLSL automatically does this. */
    object SafeAutoCast {
        def unapply(tree: r.Statement): Option[r.Statement] = tree match {
          case Apply(mthd, List(arg)) if autoCasts(mthd.symbol.fullName) => Some(arg)
          case _ => None
        }
    }
    /** Detect a GLSL operator encoded in Scala. */
    object Operator {
        /** Returns: operator, lhs, rhs. */
        def unapply(tree: r.Statement): Option[(String, r.Statement, r.Statement)] = tree match {
            // Handle operators that have implicit params...  [foo.op(bar)(implicits)]
            case Apply(Apply(mthd @ Select(lhs, _), List(rhs)), _) if operatorTransforms.contains(mthd.symbol.fullName) => 
              Some((operatorTransforms(mthd.symbol.fullName), lhs, rhs))
            // Handle operators without implicit params....
            case Apply(mthd @ Select(lhs, _), List(rhs)) if operatorTransforms.contains(mthd.symbol.fullName) => 
              Some((operatorTransforms(mthd.symbol.fullName), lhs, rhs))
            case _ => None
        }
    }
    /** Detect a GLSL constructor in Scala. */
    object Constructor {
        /** Returns: type, args */
        def unapply(tree: r.Statement): Option[(String, List[r.Statement])] = tree match {
            case Apply(term @ Apply(_, args), _) if typeApplyMethods.contains(term.symbol.fullName) =>
              Some((typeApplyMethods(term.symbol.fullName), args))
            case _ => None
        }
    }
    /** Detect GLSL built-in functions. */
    object BuiltinFunction {
        /** Returns: operation, args */
        def unapply(tree: r.Statement): Option[(String, List[r.Statement])] = tree match {
            // Handle java.lang.Math methods (todo - figure a better way to handle all built-in methods)
            case Apply(Select(ref, mthd), List(lhs,rhs)) if ref.symbol.fullName == "java.lang.Math" && javaMathOperations(mthd) =>
              Some((mthd, List(lhs, rhs)))
            // TODO - lookup for all methods...
            case Apply(term @ Select(ref, "normalize"), List(_,_)) if term.symbol.fullName == "com.jsuereth.gl.math.Vec3.normalize" => 
              Some(("normalize", List(ref)))
            // Handle dot products on vectors
            // TODO - check the symbol of the method.
            case Apply(Apply(mthd @ Select(lhs, "dot"), List(rhs)), _) => 
              Some(("dot", List(lhs,rhs)))
            // Handle Sampler2D methods  
            case  Erased(Apply(Apply(Ident("texture"), List(ref)), List(arg))) if ref.tpe <:< typeOf[Texture2D] =>
              Some("texture", List(ref, arg))
            // Handle our built-int math operations.  TODO - lock this down to NOT be so flexible...
            case Apply(Apply(TypeApply(Ident(mathOp), _), args), /* Implicit witnesses */_) if ourMathOperations(mathOp) =>
              Some((mathOp, args))
            // Handle instance methods for inverse/transpose.
            // First, methods that have implicit args.
            case Apply(term @ Select(ref, op), _) if matrixOperations(op) && term.symbol.fullName.contains("Matrix") => 
              Some((op, List(ref)))
            // Next methods with no args.
            case term @ Select(ref, op) if matrixOperations(op) && term.symbol.fullName.contains("Matrix") =>
              Some((op, List(ref)))
            case _ => None
        }
    }

    def constantToString(c: r.Constant): String = {
        import r._
        c match {
            case Constant(null) => "null"
            case Constant(_: Unit) => ""
            case Constant(n) => n.toString
        }
    }

    def convertExpr(tree: r.Statement)(using env: StmtConvertorEnv): Expr = tree match {
        case Uniform(name, tpe) => 
          env.recordUniform(name, tpe)
          Expr.Id(name)
        case UniformStruct(name, st) =>
          env.recordStruct(st)
          env.recordUniform(name, st.name)
          Expr.Id(name)
        case SafeAutoCast(arg) => convertExpr(arg)
        case BuiltinFunction(name, args) => Expr.MethodCall(name, args.map(convertExpr))
        case Operator(name, lhs, rhs) => Expr.Operator(name, convertExpr(lhs), convertExpr(rhs))
        case Constructor(name, args) => Expr.MethodCall(name, args.map(convertExpr))
        case Ident(localRef) => Expr.Id(localRef)
        case If(cond, lhs, rhs) => Expr.Terenary(convertExpr(cond), convertExpr(lhs), convertExpr(rhs))
        // Erased methods...
        case Select(expr, "toFloat") => convertExpr(expr)
        case Select(term, mthd) => Expr.Select(convertExpr(term), mthd)
        // TODO - actual encode literals in AST?
        case Literal(constant) => Expr.Id(constantToString(constant))
        // TODO - error message for everything else.
        //case Inlined(_, _, blck) => convertExpr(blck)
        case _ => 
           summon[QuoteContext].error(s"Unable to convert to expr: ${tree.show}\n\nfull tree: $tree\n\nPlease file a bug with this context.")
           Expr.Id("")
    }
    def convertStmt(tree: r.Statement)(using env: StmtConvertorEnv): codegen.Statement = tree match {
        // assign
        case Output(name, tpe, exp) =>
          env.recordOutput(name, tpe, 0) // TODO - location
          codegen.Statement.Assign(name, convertExpr(exp))
        case Input(name, tpe, location) => 
          env.recordInput(name, tpe, location)
          codegen.Statement.Effect(Expr.Id("")) // TODO - don't make fake statements....
        case GlPosition(value) => Statement.Assign("gl_Position", convertExpr(value))
        // local variable
        case ValDef(sym, tpe, optExpr) => codegen.Statement.LocalVariable(sym.toString, toGlslTypeFromTree(tpe), optExpr.map(convertExpr))
        //case VarDef(sym, tpe, optExpr) => Statement.LocalVariable(sym.toString, toGlslTypeFromTree(tpe), optExpr.map(convertExpr))
        // effect
        case _ => codegen.Statement.Effect(convertExpr(tree))
    }
    // TODO - handle helper methods and extract them separately as definitions.
    def walkFragmentShader(tree: r.Statement)(using env: ShaderConvertorEnv): Unit = tree match {
      case Inlined(None, stmts, exp) => walkFragmentShader(exp)
      case Block(statements, last) => 
        for(s <- (statements :+ last)) {
          walkFragmentShader(s)
        }
      // Ignore when we type a block as "Unit"
      case Typed(block, tpe) if tpe.tpe <:< typeOf[Unit] => walkFragmentShader(block)
      case other => env.addStatement(convertStmt(other))
    }

    // TODO - share some of the "walk" code with walkFragmentShader.
    def walkVertexShader(tree: r.Statement)(using env: VertexShaderConvertorEnv): Unit = tree match {
      // Ignore when we type a block as "Unit"
      case Typed(block, tpe) if tpe.tpe <:< typeOf[Unit] => walkVertexShader(block)
      case FragmentShader(exp) => 
        // TODO - make a new environment, then unify the shaders. after walking.
        walkFragmentShader(exp)(using env.fragmentEnv)
      case Inlined(None, stmts, exp) => walkVertexShader(exp)
      case Block(statements, last) => 
        for(s <- (statements :+ last)) {
          walkVertexShader(s)
        }
      case stmt => env.addStatement(convertStmt(stmt))  
    }

    // TODO - we need to return separate ASTs for vertex/fragment shaders.
    def convert(tree: r.Statement): (codegen.Ast, codegen.Ast) = {
        object env extends DefaultShaderConvertEnv with VertexShaderConvertorEnv {
            override val fragmentEnv = new DefaultShaderConvertEnv()
        }
        //summon[QuoteContext].warning(s"Found shader: $tree");
        walkVertexShader(tree)(using env)
        TransformAst(env.ast, env.fragmentEnv.ast)
    }
}