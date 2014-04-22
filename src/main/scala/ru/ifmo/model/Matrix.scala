package ru.ifmo.model

import java.util

class Matrix(m: Array[Array[Double]]) {
  val data: Array[Array[Double]] = m
  val rows: Int = m.length
  val cols: Int = m(0).length

  def mul(m: Matrix): Matrix = {
    val rows = this.rows
    val cols = m.cols
    val t = this.cols
    val result: Array[Array[Double]] = Array.ofDim(rows, cols)

    for (i <- 0 until rows; j <- 0 until cols) {
      var s = 0.0
      for (k <- 0 until t) {
        s += this.data(i)(k) * m.data(k)(j)
      }
      result(i)(j) = s
    }

    new Matrix(result)
  }

  def apply(i: Int, j: Int): Double = {
    data(i)(j)
  }

  override def toString: String = {
    data.foldLeft("")((str, array) => str + util.Arrays.toString(array) + '\n')
  }
}
