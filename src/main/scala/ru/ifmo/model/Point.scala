package ru.ifmo.model

import org.apache.commons.math3.util.FastMath

class Point(xc: Double, yc: Double, zc: Double) {
  val x: Double = xc
  val y: Double = yc
  val z: Double = zc


  override def toString: String = s"($x, $y, $z)"

  def asVector(): Vector = new Vector(x, y, z)

  def distance(that: Point) = FastMath.sqrt((this.x - that.x) * (this.x - that.x) + (this.y - that.y) * (this.y - that.y) + (this.z - that.z) * (this.z - that.z))

  def canEqual(other: Any): Boolean = other.isInstanceOf[Point]

  override def equals(other: Any): Boolean = other match {
    case that: Point =>
      (that canEqual this) &&
        x == that.x &&
        y == that.y &&
        z == that.z
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(x, y, z)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }


}
