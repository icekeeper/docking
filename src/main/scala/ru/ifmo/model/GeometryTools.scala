package ru.ifmo.model

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

  def computeTransitionMatrix(a: Point, b: Point): Matrix = {
    new Matrix(Array(
      Array(1.0, 0.0, 0.0, 0.0),
      Array(0.0, 1.0, 0.0, 0.0),
      Array(0.0, 0.0, 1.0, 0.0),
      Array(a.x - b.x, a.y - b.y, a.z - b.z, 1.0)
    ))
  }

  def transformPoint(p: Point, m: Matrix): Point = {
    val w = p.x * m(0, 3) + p.y * m(1, 3) + p.z * m(2, 3) + m(3, 3)
    new Point(
      (p.x * m(0, 0) + p.y * m(1, 0) + p.z * m(2, 0) + m(3, 0)) / w,
      (p.x * m(0, 1) + p.y * m(1, 1) + p.z * m(2, 1) + m(3, 1)) / w,
      (p.x * m(0, 2) + p.y * m(1, 2) + p.z * m(2, 2) + m(3, 2)) / w
    )
  }

  def transformVector(v: Vector, m: Matrix): Vector = {
    val w = v.x * m(0, 3) + v.y * m(1, 3) + v.z * m(2, 3) + m(3, 3)
    new Vector(
      (v.x * m(0, 0) + v.y * m(1, 0) + v.z * m(2, 0) + m(3, 0)) / w,
      (v.x * m(0, 1) + v.y * m(1, 1) + v.z * m(2, 1) + m(3, 1)) / w,
      (v.x * m(0, 2) + v.y * m(1, 2) + v.z * m(2, 2) + m(3, 2)) / w
    )
  }

  def triangleCenter(a: Point, b: Point, c: Point): Point = {
    val p1 = a.asVector()
    val p2 = b.asVector()
    val p3 = c.asVector()

    val s: Double = 2 * ((p1 sub p2) cross (p2 sub p3)).lengthSqr
    val alpha = (p2 sub p3).lengthSqr * ((p1 sub p2) dot (p1 sub p3)) / s
    val beta = (p1 sub p3).lengthSqr * ((p2 sub p1) dot (p2 sub p3)) / s
    val gamma = (p1 sub p2).lengthSqr * ((p3 sub p1) dot (p3 sub p2)) / s

    ((p1 mul alpha) add (p2 mul beta) add (p3 mul gamma)).asPoint
  }

  def triangleNormal(s: Surface, triangle: (Int, Int, Int)): Vector = {
    val firstSideVector = Vector.fromPoints(s.points(triangle._1), s.points(triangle._2))
    val secondSideVector = Vector.fromPoints(s.points(triangle._2), s.points(triangle._3))
    val normal = firstSideVector cross secondSideVector
    if ((normal dot s.normals(triangle._1)) > 0) {
      normal.unite
    } else {
      normal.mul(-1).unite
    }
  }

  def computeTriangleTransformMatrix(s1: Surface, t1: (Int, Int, Int), s2: Surface, t2: (Int, Int, Int)): Matrix = {
    val tn1: Vector = GeometryTools.triangleNormal(s1, t1)
    val tn2: Vector = GeometryTools.triangleNormal(s2, t2)
    val firstTriangle: (Point, Point, Point) = (s1.points(t1._1), s1.points(t1._2), s1.points(t1._3))
    val secondTriangle: (Point, Point, Point) = (s2.points(t2._1), s2.points(t2._2), s2.points(t2._3))
    computeTrianglesTransformMatrix(tn1, tn2, firstTriangle, secondTriangle)
  }

  def computeTrianglesTransformMatrix(tn1: Vector, tn2: Vector, t1: (Point, Point, Point), t2: (Point, Point, Point)): Matrix = {
    val firstRotationMatrix: Matrix = GeometryTools.computeRotationMatrix(tn1, tn2.mul(-1))

    val firstCenter: Point = GeometryTools.triangleCenter(t1._1, t1._2, t1._3)
    val secondCenter: Point = GeometryTools.triangleCenter(t2._1, t2._2, t2._3)

    val firstPointVector: Vector = Vector.fromPoints(t1._1, firstCenter)
    val secondPointVector: Vector = Vector.fromPoints(t2._1, secondCenter)

    val rotatedSecondVector = GeometryTools.transformVector(secondPointVector, firstRotationMatrix)
    val secondRotationMatrix = GeometryTools.computeRotationMatrix(firstPointVector, rotatedSecondVector)
    val rotationMatrix = firstRotationMatrix mul secondRotationMatrix

    val movedSecondCenter: Point = GeometryTools.transformPoint(secondCenter, rotationMatrix)
    val transitionMatrix = GeometryTools.computeTransitionMatrix(firstCenter, movedSecondCenter)

    rotationMatrix mul transitionMatrix
  }
}
