package com.jsuereth.gl
package test

import org.junit.Assert

def cleanLineEndings(in: String): String =
  in.replaceAll("\r\n", "\n")

def assertCleanEquals(expected: String, actual: String): Unit =
  Assert.assertEquals(cleanLineEndings(expected), cleanLineEndings(actual))