package ru.ifmo.model

import ru.ifmo.docking.geometry.Vector
import ru.ifmo.docking.model.Matrix

object GeometryTools {

  def computeRotationMatrix(a: Vector, b: Vector): Matrix = {
    val sin = (a cross b).length / (a.length * b.length)
    val cos = (a dot b) / (a.length * b.length)

    if (Math.abs(sin) < 1e-10) {
      if (cos > 0) {
        new Matrix(Array(
          Array(1.0, 0.0, 0.0, 0.0),
          Array(0.0, 1.0, 0.0, 0.0),
          Array(0.0, 0.0, 1.0, 0.0),
          Array(0.0, 0.0, 0.0, 1.0)
        ))
      } else {
        val normal = a.unite
        val x = normal.x
        val y = normal.y
        val z = normal.z

        new Matrix(Array(
          Array(1 - 2 * x * x, -2 * x * y, -2 * x * z, 0.0),
          Array(-2 * x * y, 1 - 2 * y * y, -2 * y * z, 0.0),
          Array(-2 * x * z, -2 * y * z, 1 - 2 * z * z, 0.0),
          Array(0.0, 0.0, 0.0, 1.0)
        ))
      }
    } else {
      val rotationAxis = (a cross b).unite
      val x: Double = rotationAxis.x
      val y: Double = rotationAxis.y
      val z: Double = rotationAxis.z

      new Matrix(Array(
        Array(cos + (1 - cos) * x * x, (1 - cos) * x * y - sin * z, (1 - cos) * x * z + sin * y, 0.0),
        Array((1 - cos) * y * x + sin * z, cos + (1 - cos) * y * y, (1 - cos) * y * z - sin * x, 0.0),
        Array((1 - cos) * z * x - sin * y, (1 - cos) * z * y + sin * x, cos + (1 - cos) * z * z, 0.0),
        Array(0.0, 0.0, 0.0, 1.0)
      ))
    }
  }

}
