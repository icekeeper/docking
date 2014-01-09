package ru.ifmo.model

import org.apache.commons.math3.util.FastMath

class Vector(xc: Double, yc: Double, zc: Double) {
  val x: Double = xc
  val y: Double = yc
  val z: Double = zc


  def length: Double = FastMath.sqrt(this dot this)

  def unite: Vector = new Vector(x / length, y / length, z / length)

  def dot(that: Vector): Double = this.x * that.x + this.y * that.y + this.z * that.z


  def canEqual(other: Any): Boolean = other.isInstanceOf[Vector]

  override def equals(other: Any): Boolean = other match {
    case that: Vector =>
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

object Vector {
  def fromPoints(first: Point, second: Point) = new Vector(first.x - second.x, first.y - second.y, first.z - second.z)
}

