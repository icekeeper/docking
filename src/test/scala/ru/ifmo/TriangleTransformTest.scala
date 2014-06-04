package ru.ifmo

import org.scalatest.{Matchers, FlatSpec}
import ru.ifmo.docking.geometry.{Geometry, Point}
import scala.collection.JavaConversions._

class TriangleTransformTest extends FlatSpec with Matchers {

  "Attempt to rotate triangle" should "be successful" in {
    val p1 = new Point(3.0, 5.0, 2.0)
    val p2 = new Point(5.0, 5.0, 6.0)
    val p3 = new Point(8.0, 5.0, 2.0)

    val p4 = new Point(3.0, 5.0, 10.0)
    val p5 = new Point(7.0, 7.0, 10.0)
    val p6 = new Point(3.0, 10.0, 10.0)

    val matrix = Geometry.findRmsdOptimalTransformationMatrix(List(p4, p5, p6), List(p1, p2, p3))

    val pr1 = Geometry.transformPoint(p4, matrix)
    val pr2 = Geometry.transformPoint(p5, matrix)
    val pr3 = Geometry.transformPoint(p6, matrix)


    pr1.x should equal(3.0 +- 1e-8)
    pr1.y should equal(5.0 +- 1e-8)
    pr1.z should equal(2.0 +- 1e-8)

    pr2.x should equal(5.0 +- 1e-8)
    pr2.y should equal(5.0 +- 1e-8)
    pr2.z should equal(6.0 +- 1e-8)

    pr3.x should equal(8.0 +- 1e-8)
    pr3.y should equal(5.0 +- 1e-8)
    pr3.z should equal(2.0 +- 1e-8)

  }

  "Points rotation that minimize RMSD" should "be correct" in {
    val p1 = new Point(0.0, 0.0, 1.0)
    val p2 = new Point(0.0, 2.0, 0.0)
    val p3 = new Point(2.0, 2.0, 0.0)
    val p4 = new Point(2.0, 0.0, 0.0)

    val p5 = new Point(1.0, 0.0, 0.0)
    val p6 = new Point(0.0, 0.0, 2.0)
    val p7 = new Point(0.0, 2.0, 2.0)
    val p8 = new Point(0.0, 2.0, 0.0)

    val rmsd = Geometry.rmsd(List(p1, p2, p3, p4), List(p5, p6, p7, p8))

    rmsd should equal(0.0 +- 1e-15)
  }

}
