package ru.ifmo.model

import org.apache.commons.math3.util.FastMath
import java.util

class SpinImage(geometry: Array[Array[Double]], electricity: Array[Array[Double]], lipophilicity: Array[Array[Double]]) {
  val geometryBins: Array[Array[Double]] = geometry
  val electricityBins: Array[Array[Double]] = electricity
  val lipophilicityBins: Array[Array[Double]] = lipophilicity


  override def toString: String = {
    geometryBins.foldLeft("geometry: \n")((str, array) => str + util.Arrays.toString(array) + '\n') +
      electricityBins.foldLeft("electricity: \n")((str, array) => str + util.Arrays.toString(array) + '\n') +
      lipophilicityBins.foldLeft("lipophilicity: \n")((str, array) => str + util.Arrays.toString(array) + '\n')
  }

  def correlation(that: SpinImage): Double = {
    val geometryCorrelation: Double = SpinImage.correlation(this.geometryBins, that.geometryBins)._2
    val electricityCorrelation: Double = SpinImage.correlation(this.electricityBins, that.electricityBins, -1)._2
    val lipophilicityCorrelation: Double = SpinImage.correlation(this.lipophilicityBins, that.lipophilicityBins)._2

    geometryCorrelation * 0.2 + electricityCorrelation * 0.4 + lipophilicityCorrelation * 0.4
  }

  def similarity(that: SpinImage): Double = {
    val geometryCorrelation: (Long, Double) = SpinImage.correlation(this.geometryBins, that.geometryBins)
    val electricityCorrelation: Double = SpinImage.correlation(this.electricityBins, that.electricityBins, -1)._2
    val lipophilicityCorrelation: Double = SpinImage.correlation(this.lipophilicityBins, that.lipophilicityBins)._2

    val c = geometryCorrelation._2 * 0.4 + electricityCorrelation * 0.2 + lipophilicityCorrelation * 0.4

    if (geometryCorrelation._1 < 4) 0 else FastMath.pow(FastMath.atanh(c), 2) - 3 * (1 / (geometryCorrelation._1 - 3))
  }

}

object SpinImage {

  def compute(pointIndex: Int, surface: Surface, r: Double, b: Double): SpinImage = {
    val iMax: Int = FastMath.round(FastMath.floor(2 * r / b)).toInt + 1
    val jMax: Int = FastMath.round(FastMath.floor(r / b)).toInt + 1

    val gBins: Array[Array[Double]] = Array.ofDim(iMax + 1, jMax + 1)
    val eBins: Array[Array[Double]] = Array.ofDim(iMax + 1, jMax + 1)
    val lBins: Array[Array[Double]] = Array.ofDim(iMax + 1, jMax + 1)

    def updateBins(bins: Array[Array[Double]], index: (Int, Int), bil: (Double, Double), value: Double) = {
      if (!value.isNaN) {
        bins(index._1)(index._2) += (1 - bil._1) * (1 - bil._2) * value
        bins(index._1 + 1)(index._2) += bil._1 * (1 - bil._2) * value
        bins(index._1)(index._2 + 1) += (1 - bil._1) * bil._2 * value
        bins(index._1 + 1)(index._2 + 1) += bil._1 * bil._2 * value
      }
    }

    val point = surface.points(pointIndex)
    (surface.points.iterator withFilter (p => (p distance point) < r) zipWithIndex) foreach {
      case (p, index) =>
        val coordinates: (Double, Double) = computeOrientedCoordinates(point, p, surface.normals(pointIndex))

        val i: Int = FastMath.round(FastMath.floor((r - coordinates._2) / b)).toInt
        val j: Int = FastMath.round(FastMath.floor(coordinates._1 / b)).toInt

        val bilA = (r - coordinates._2) / b - i
        val bilB = coordinates._1 / b - j

        updateBins(gBins, (i, j), (bilA, bilB), 1)
        updateBins(eBins, (i, j), (bilA, bilB), surface.electrostaticPotentials(index))
        updateBins(lBins, (i, j), (bilA, bilB), surface.lipophilicPotentials(index))
    }
    new SpinImage(gBins, eBins, lBins)
  }

  def computeOrientedCoordinates(basis: Point, target: Point, normal: Vector): (Double, Double) = {
    val p = Vector.fromPoints(target, basis)
    val b = normal dot p
    val a = FastMath.sqrt(p.length * p.length - b * b)
    (a, b)
  }

  private def correlation(first: Array[Array[Double]], second: Array[Array[Double]], secondMultiplier: Int = 1): (Long, Double) = {
    var sumP: Double = 0
    var sumQ: Double = 0
    var sumPP: Double = 0
    var sumQQ: Double = 0
    var sumPQ: Double = 0
    var N: Long = 0

    (first.iterator.flatten zip second.reverseIterator.flatten) foreach {
      case (p, q) => if (p != 0 || q != 0) {
        N += 1
        sumP += p
        sumQ += q * secondMultiplier
        sumPP += p * p
        sumQQ += q * q
        sumPQ += p * q * secondMultiplier
      }
    }

    if (N == 0 || sumP == 0 || sumQ == 0) (0, 0) else (N, (N * sumPQ - sumP * sumQ) / FastMath.sqrt((N * sumPP - sumP * sumP) * (N * sumQQ - sumQ * sumQ)))
  }

}


