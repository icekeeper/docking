package ru.ifmo.model

import org.apache.commons.math3.util.FastMath

class Vector(xc: Double, yc: Double, zc: Double) {
  val x: Double = xc
  val y: Double = yc
  val z: Double = zc

  def asPoint: Point = new Point(x, y, z)

  def length: Double = FastMath.sqrt(this dot this)

  def lengthSqr: Double = this dot this

  def unite: Vector = new Vector(x / length, y / length, z / length)

  def dot(that: Vector): Double = this.x * that.x + this.y * that.y + this.z * that.z

  def cross(that: Vector): Vector = new Vector(this.y * that.z - this.z * that.y, this.z * that.x - this.x * that.z, this.x * that.y - this.y * that.x)

  def sub(that: Vector): Vector = new Vector(x - that.x, y - that.y, z - that.z)

  def add(that: Vector): Vector = new Vector(x + that.x, y + that.y, z + that.z)

  def mul(k: Double): Vector = new Vector(k * x, k * y, k * z)

  def canEqual(other: Any): Boolean = other.isInstanceOf[Vector]

  def mul(m: Matrix): Vector = {
    val t: Matrix = new Matrix(Array(Array(x, y, z))) mul m
    new Vector(t(0, 0), t(0, 1), t(0, 2))
  }

  override def toString: String = s"($x, $y, $z)"

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

