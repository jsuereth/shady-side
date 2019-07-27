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

package com.jsuereth.gl.shaders.codegen

case class Ast(decls: Seq[Declaration], version: String = "300 es") {
  // TODO - which version ddo we target?
  // TODO - Allow specifying default precision for opengl ES
  def toProgramString: String = 
    
    s"""|#version $version
        |
        |precision highp float;
        |precision highp int;
        |
        |${decls.map(_.toProgramString).mkString("\n")}""".stripMargin('|')
    //s"${decls.map(_.toProgramString).mkString("\n")}"
}

// TODO - a better encoding of variables from GLSL
/** Declarations available in GLSL at the top-level. */
enum Declaration {
  case Uniform(name: String, tpe: String)
  // TODO - better layout controls
  case Input(name: String, tpe: String, location: Option[Int] = None)
  case Output(name: String, tpe: String, location: Option[Int] = None)
  // TODO - method with args...
  case Method(name: String, tpe: String, block: Seq[Statement])
  /** The definitiion of a struct. */
  case Struct(name: String, members: Seq[StructMember])

  def toProgramString: String = this match {
    case Uniform(name, tpe) => s"uniform $tpe $name;"
    case Input(name, tpe, Some(location)) => s"layout (location = $location) in $tpe $name;"
    case Input(name, tpe, None) => s"in $tpe $name;"
    case Output(name, tpe, Some(location)) => s"layout (location = $location) out $tpe $name;"
    case Output(name, tpe, None) => s"out $tpe $name;"
    case Method(name, tpe, block) => s"$tpe $name() {\n  ${block.map(_.toProgramString).mkString("\n  ")}\n}"
    case Struct(name, members) => s"struct $name {\n  ${members.map(_.toProgramString).mkString("\n  ")}\n};"
  }
}

/** A member of a structure. */
case class StructMember(name: String, tpe: String) {
  def toProgramString: String = s"$tpe $name;"
}

/** Statements in the GLSL language. */
enum Statement {
  case LocalVariable(name: String, tpe: String, initialValue: Option[Expr])
  case Effect(expr: Expr)
  case Assign(ref: String, value: Expr)

  def toProgramString: String = this match {
    // Here is a hack to remove empty statements we use as placehoders....
    case Effect(expr) => s"${expr.toProgramString};"
    case LocalVariable(name, tpe, None) => s"$tpe $name;"
    case LocalVariable(name, tpe, Some(expr)) => s"$tpe $name = ${expr.toProgramString};"
    case Assign(ref, value) => s"$ref = ${value.toProgramString};"
  }
}

// TODO - encode the valid range of expressions in GLSL
/** Valid expressions (rvalues) in GLSL */
enum Expr {
  case MethodCall(name: String, args: Seq[Expr])
  case Id(name: String)
  case Terenary(cond: Expr, lhs: Expr, rhs: Expr)
  case Operator(name: String, lhs: Expr, rhs: Expr)
  case Select(lhs: Expr, property: String)
  case Negate(expr: Expr)


  def toProgramString: String = this match {
    case MethodCall(name, args) => s"$name(${args.map(_.toProgramString).mkString("", ",", "")})"
    case Id(name) => name
    case Operator(name, lhs, rhs) => s"(${lhs.toProgramString} $name ${rhs.toProgramString})"
    case Negate(expr) => s"-(${expr.toProgramString})"
    case Terenary(cond, lhs, rhs) => s"(${cond.toProgramString}) ? (${lhs.toProgramString}) : (${rhs.toProgramString})"
    case Select(lhs, rhs) => s"(${lhs.toProgramString}).$rhs"
  }
}