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

def Refs(ast: Ast | Declaration | Statement | Expr): Set[String] = ast match {
  case x: Ast => x.decls.iterator.flatMap(Refs).toSet
  case Declaration.Method(_, _, block) => block.flatMap(Refs).toSet
  case Statement.Effect(expr) => Refs(expr)
  case Statement.Assign(_, expr) => Refs(expr)
  case Statement.LocalVariable(_, _, Some(expr)) => Refs(expr)
  case Expr.MethodCall(name, args) => args.flatMap(Refs).toSet
  case Expr.Id(name) => Set(name)
  case Expr.Operator(name, lhs, rhs) => Refs(lhs) ++ Refs(rhs)
  case Expr.Negate(expr) => Refs(expr)
  case Expr.Terenary(cond, lhs, rhs) => Refs(cond) ++ Refs(lhs) ++ Refs(rhs)
  case Expr.Select(lhs, rhs) => Refs(lhs)
  case _ => Set()
}

/** An overridable transformer that provides basic "transform everything" impl. */
class AstTransformer {
    def transform(ast: Ast): Ast = Ast(ast.decls.map(transform))
    def transform(decl: Declaration): Declaration = decl match {
        case Declaration.Method(name, tpe, block) => Declaration.Method(name, tpe, block.map(transform))
        case _ => decl
    }
    def transform(stmt: Statement): Statement = stmt match {
      case Statement.Effect(expr) => Statement.Effect(transform(expr))
      case Statement.Assign(name, expr) => Statement.Assign(name, transform(expr))
      case Statement.LocalVariable(name, tpe, optExpr) => Statement.LocalVariable(name, tpe, optExpr.map(transform))
    }
    def transform(expr: Expr): Expr = expr match {
      case Expr.MethodCall(name, args) => Expr.MethodCall(name, args.map(transform))
      case Expr.Id(_) => expr
      case Expr.Operator(name, lhs, rhs) => Expr.Operator(name, transform(lhs), transform(rhs))
      case Expr.Negate(expr) => Expr.Negate(transform(expr))
      case Expr.Terenary(cond, lhs, rhs) => Expr.Terenary(transform(cond), transform(lhs), transform(rhs))
      case Expr.Select(lhs, rhs) => Expr.Select(transform(lhs), rhs)
    }
}

object PlaceholderRemover extends AstTransformer {
    def isPlaceholder(e: Expr | Statement): Boolean = e match {
        case Statement.Effect(e) => isPlaceholder(e)
        case Expr.Id("") => true
        case _ => false
    }

    override def transform(decl: Declaration): Declaration = decl match {
        case Declaration.Method(name, tpe, block) => Declaration.Method(name, tpe, block.filterNot(isPlaceholder))
        case _ => decl
    }
}

class OutputVariableTransformer(vars: Set[(String,String)]) extends AstTransformer {
  override def transform(ast: Ast): Ast = {
    val outs = for((name, tpe) <- vars) yield Declaration.Output(name, tpe, None)
    Ast(outs.toSeq ++ ast.decls.map(transform))
  }
  override def transform(stmt: Statement): Statement = stmt match {
      case Statement.LocalVariable(name, tpe, Some(expr)) if vars(name -> tpe) => Statement.Assign(name, expr) 
      case Statement.LocalVariable(name, tpe, None) if vars(name -> tpe) => Statement.Effect(Expr.Id("")) // TODO - alternative way to get rid of statements.
      case _ => super.transform(stmt)
  }
}

class InputVariableTransformer(vars: Set[(String,String)]) extends AstTransformer {
  override def transform(ast: Ast): Ast = {
    val ins = for((name, tpe) <- vars) yield Declaration.Input(name, tpe, None)
    Ast(ins.toSeq ++ ast.decls.map(transform))
  }
  override def transform(stmt: Statement): Statement = stmt match {
      case Statement.LocalVariable(name, tpe, Some(expr)) if vars(name -> tpe) => Statement.Assign(name, expr) 
      case Statement.LocalVariable(name, tpe, None) if vars(name -> tpe) => Statement.Effect(Expr.Id("")) // TODO - alternative way to get rid of statements.
      case _ => super.transform(stmt)
  }
}

/** Autocorrects shared reference variables between vertex + pixel to be output/input appropriately. */
def TransformAst(vertex: Ast, pixel: Ast): (Ast, Ast) = {
    // TODO - this is a really horrible implementation, not very safe/good.
    val localVariables =
      (for {
          Declaration.Method(_,_, block) <- vertex.decls.iterator.filter(_.isInstanceOf[Declaration.Method])
          Statement.LocalVariable(name, tpe, _) <- block.iterator.filter(_.isInstanceOf[Statement.LocalVariable])
      } yield name -> tpe).toMap
    val sharedVariables =
      for {
          name <- Refs(pixel)
          if localVariables.contains(name)
      } yield name -> localVariables(name)
    val vTransform = new OutputVariableTransformer(sharedVariables)  
    val fTransform = new InputVariableTransformer(sharedVariables)
    //throw new Exception(s"Found shared references: ${sharedVariables.mkString(", ")}")  
    (PlaceholderRemover.transform(vTransform.transform(vertex)),PlaceholderRemover.transform(fTransform.transform(pixel)))
}