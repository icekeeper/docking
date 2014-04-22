package ru.ifmo

import org.scalatest.{Matchers, FlatSpec}
import ru.ifmo.model.{GeometryTools, Matrix, Vector}

class RotationTest extends FlatSpec with Matchers {

  "Vectors with sharp angle" should "be rotated correctly" in {
    val a = new Vector(1, 2, 3)
    val b = new Vector(6, 4, 2)

    val rotationMatrix: Matrix = GeometryTools.computeRotationMatrix(a, b)

    val rotated: Vector = b mul rotationMatrix

    rotated.x should equal(2.0 +- 1e-10)
    rotated.y should equal(4.0 +- 1e-10)
    rotated.z should equal(6.0 +- 1e-10)
  }

  "Vectors with obtuse angle" should "be rotated correctly" in {
    val a = new Vector(1, 2, 3)
    val b = new Vector(-6, -4, -2)

    val rotationMatrix: Matrix = GeometryTools.computeRotationMatrix(a, b)

    val rotated: Vector = b mul rotationMatrix

    rotated.x should equal(2.0 +- 1e-10)
    rotated.y should equal(4.0 +- 1e-10)
    rotated.z should equal(6.0 +- 1e-10)
  }

  "Opposite vector" should "be rotated correctly" in {
    val a = new Vector(1, 2, 3)
    val b = new Vector(-2, -4, -6)

    val rotationMatrix: Matrix = GeometryTools.computeRotationMatrix(a, b)

    val rotated: Vector = b mul rotationMatrix

    rotated.x should equal(2.0 +- 1e-10)
    rotated.y should equal(4.0 +- 1e-10)
    rotated.z should equal(6.0 +- 1e-10)
  }

  "Equal vector" should "be rotated correctly" in {
    val a = new Vector(1, 2, 3)
    val b = new Vector(2, 4, 6)

    val rotationMatrix: Matrix = GeometryTools.computeRotationMatrix(a, b)

    val rotated: Vector = b mul rotationMatrix

    rotated.x should equal(2.0 +- 1e-10)
    rotated.y should equal(4.0 +- 1e-10)
    rotated.z should equal(6.0 +- 1e-10)
  }
}
