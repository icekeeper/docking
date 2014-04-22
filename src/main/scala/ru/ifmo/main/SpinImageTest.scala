package ru.ifmo.main

import java.io.{PrintWriter, File}
import ru.ifmo.model._
import scala.io._
import scala.collection.mutable.ArrayBuffer

object SpinImageTest {

  def main(args: Array[String]) {
    val firstDir = s"data/${args(0)}_data"
    val secondDir = s"data/${args(1)}_data"

    val first = Surface.read(new File(firstDir, s"${args(0)}.obj"), new File(firstDir, s"${args(0)}_pot.csv"), new File(firstDir, s"${args(0)}_lip.csv"))
    val second = Surface.read(new File(secondDir, s"${args(1)}.obj"), new File(secondDir, s"${args(1)}_pot.csv"), new File(secondDir, s"${args(1)}_lip.csv"))

    //                printClosesPoints(first, second)
    //    printClosesPointsSimilarity(first, second)
    //    printMaxSimilarity(first, second)
    printPLYFiles(first, second)
  }

  def printPLYFiles(first: Surface, second: Surface) {
    val correlationPairs = findMaxCorrelationPairs(first, second)

    val firstColors: Seq[(Int, Int, Int)] = first.points.indices.map(i => if (correlationPairs.exists(p => p._1 == i)) (255, 0, 0) else (0, 0, 0))
    val secondColors: Seq[(Int, Int, Int)] = second.points.indices.map(i => if (correlationPairs.exists(p => p._2 == i)) (255, 0, 0) else (0, 0, 0))

    printSurfaceAsPLY(first, firstColors, new File("first.ply"))
    printSurfaceAsPLY(second, secondColors, new File("second.ply"))
    printSurfaceAsPLY(first, second, correlationPairs, new File("combined.ply"))

  }

  def printMaxSimilarity(first: Surface, second: Surface) {
    val start = System.currentTimeMillis()

    val correlationPairs = findMaxCorrelationPairs(first, second)

    val firstIndex: Int = correlationPairs.indexWhere(p => (first.points(p._1) distance second.points(p._2)) < 1.0)
    val secondIndex: Int = correlationPairs.indexWhere(p => (first.points(p._1) distance second.points(p._2)) < 2.0)

    println(s"Point close by 2.0 on position $secondIndex: ${correlationPairs(secondIndex)}")
    println(s"Point close by 1.0 on position $firstIndex: ${correlationPairs(firstIndex)}")

    println(s"Correlation bound: ${correlationPairs.last._3}")

    def pairDist(pairIndex: Int): Double = {
      val pair = correlationPairs(pairIndex)
      first.points(pair._1) distance second.points(pair._2)
    }

    def pointDist(firstIndex: Int, secondIndex: Int): Double = {
      val firstPair = correlationPairs(firstIndex)
      val secondPair = correlationPairs(secondIndex)
      second.points(firstPair._2) distance second.points(secondPair._2)
    }

    def isGoodPair(firstIndex: Int, secondIndex: Int) = {
      val firstPair = correlationPairs(firstIndex)
      val secondPair = correlationPairs(secondIndex)
      val firstDist = first.points(firstPair._1) distance first.points(secondPair._1)
      val secondDist = second.points(firstPair._2) distance second.points(secondPair._2)
      val delta: Double = Math.abs(firstDist - secondDist)

      val firstDotProduct = first.normals(firstPair._1) dot first.normals(secondPair._1)
      val secondDotProduct = second.normals(firstPair._2) dot second.normals(secondPair._2)

      (firstPair._1 != secondPair._1
        && firstPair._2 != secondPair._2
        && delta < 1
        && firstDotProduct > 0
        && secondDotProduct > 0)
    }

    val indices = (0 until correlationPairs.size).par

    val lines = indices.flatMap {
      a: Int => {
        (a + 1 until correlationPairs.size).filter(isGoodPair(a, _)).map(x => (a, x))
      }
    }.seq.toSet

    val counts: Array[Int] = Array.ofDim(indices.size)

    lines.foreach {
      case (a, b) =>
        counts(a) += 1
        counts(b) += 1
    }

    println(s"Got ${lines.size} lines")

    println(s"Min lines count for point: ${counts.min}")
    println(s"Max lines count for point: ${counts.max}")
    println(s"Avg lines count for point: ${1.0 * counts.sum / counts.length}")
    println(s"Med lines count for point: ${counts.sorted(Ordering.Int)(counts.length / 2)}")

    //    def maxPointsDist(firstIndex: Int, secondIndex: Int) = {
    //      val firstPair = correlationPairs(firstIndex)
    //      val secondPair = correlationPairs(secondIndex)
    //      val firstDist = first.points(firstPair._1) distance first.points(secondPair._1)
    //      val secondDist = second.points(firstPair._2) distance second.points(secondPair._2)
    //      Math.max(firstDist, secondDist)
    //    }
    //
    //    def delta(firstIndex: Int, secondIndex: Int) = {
    //      val firstPair = correlationPairs(firstIndex)
    //      val secondPair = correlationPairs(secondIndex)
    //      val firstDist = first.points(firstPair._1) distance first.points(secondPair._1)
    //      val secondDist = second.points(firstPair._2) distance second.points(secondPair._2)
    //      Math.abs(firstDist - secondDist)
    //    }
    //
    //    val maxLine: (Int, Int) = lines.maxBy(x => maxPointsDist(x._1, x._2))
    //    val minLine: (Int, Int) = lines.minBy(x => maxPointsDist(x._1, x._2))
    //    val sumLine = lines.foldLeft(0.0)((a, x) => a + maxPointsDist(x._1, x._2))
    //
    //    println(s"Max line length: ${maxPointsDist(maxLine._1, maxLine._2)}")
    //    println(s"Min line length: ${maxPointsDist(minLine._1, minLine._2)}")
    //    println(s"Avg line length: ${sumLine / lines.size}")
    //
    //    val maxDelta: (Int, Int) = lines.maxBy(x => delta(x._1, x._2))
    //    val minDelta: (Int, Int) = lines.minBy(x => delta(x._1, x._2))
    //    val sumDelta = lines.foldLeft(0.0)((a, x) => a + delta(x._1, x._2))
    //
    //    println(s"Max delta: ${delta(maxDelta._1, maxDelta._2)}")
    //    println(s"Min delta: ${delta(minDelta._1, minDelta._2)}")
    //    println(s"Avg delta: ${sumDelta / lines.size}")

    val maxPair: Int = counts.indexOf(counts.max)
    println(s"Max pair index: $maxPair")

    def fitsToBasket(buffer: ArrayBuffer[(Int, Int)], pair: (Int, Int), p: Int): Boolean = {
      buffer.forall(x => {
        val p1 = if (x._1 == p) x._2 else x._1
        val p2 = if (pair._1 == p) pair._2 else pair._1
        lines.contains((p1, p2)) || lines.contains((p2, p1))
      })
    }

    def bigBasketsCount(pair: Int): Int = {
      val baskets = scala.collection.mutable.ArrayBuffer.empty[ArrayBuffer[(Int, Int)]]
      lines.filter(x => x._1 == pair || x._2 == pair).foreach {
        t =>
          val basket: Option[ArrayBuffer[(Int, Int)]] = baskets.find(fitsToBasket(_, t, pair))
          if (basket.isDefined) {
            basket.get += t
          } else {
            val buffer: ArrayBuffer[(Int, Int)] = scala.collection.mutable.ArrayBuffer.empty[(Int, Int)]
            baskets += buffer
            buffer += t
          }
      }
      baskets.count(b => b.size >= 10)
    }

    val bigBaskets: Seq[(Int, Int)] = indices.filter(i => counts(i) > 1200).map(i => (bigBasketsCount(i), i)).seq
    println(s"Big baskets computed for ${bigBaskets.size} pairs")

    val sortedBaskets = bigBaskets.sortBy(-_._1)
    sortedBaskets.take(100).foreach(p => {
      println(s"Pair with index ${p._2} has ${p._1} baskets and ${counts(p._2)} lines and dist: ${pairDist(p._2)}")
    })



    //    val triangles = indices.flatMap {
    //      a: Int => {
    //        val candidates = (a + 1 until correlationPairs.size).filter(isGoodPair(a, _))
    //
    //        val buffer = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Int)]
    //
    //        for (bi <- candidates.indices; ci <- bi + 1 until candidates.size) {
    //          val b = candidates(bi)
    //          val c = candidates(ci)
    //          if (isGoodPair(b, c)) {
    //            buffer += ((a, b, c))
    //          }
    //        }
    //
    //        buffer
    //      }
    //    }
    //
    //    println(s"Triangles count: ${triangles.size} ${time()}")
    //
    //    val minTriangle = triangles minBy {
    //      case (a, b, c) => pairDist(a) + pairDist(b) + pairDist(c)
    //    }
    //
    //    val maxTriangle = triangles maxBy {
    //      case (a, b, c) => correlationPairs(a)._3 + correlationPairs(b)._3 + correlationPairs(c)._3
    //    }
    //
    //    val maxSquareTriangle = triangles maxBy {
    //      case (a, b, c) => pointDist(a, b) + pointDist(b, c) + pointDist(c, a)
    //    }
    //
    //    println(s"Triangle with min distance has distances: ${pairDist(minTriangle._1)} ${pairDist(minTriangle._2)} ${pairDist(minTriangle._3)} ${time()}")
    //    println(s"Triangle with max correlation has distances: ${pairDist(minTriangle._1)} ${pairDist(minTriangle._2)} ${pairDist(minTriangle._3)} ${time()}")
    //
    //    val minTriangleIdiciesFirst = (correlationPairs(minTriangle._1)._1, correlationPairs(minTriangle._2)._1, correlationPairs(minTriangle._3)._1)
    //    val minTriangleIdiciesSecond = (correlationPairs(minTriangle._1)._2, correlationPairs(minTriangle._2)._2, correlationPairs(minTriangle._3)._2)
    //
    //    val minTriangleMatrix: Matrix = GeometryTools.computeTriangleTransformMatrix(first, minTriangleIdiciesFirst, second, minTriangleIdiciesSecond)
    //    println(s"Min triangle transform matrix: $minTriangleMatrix")
    //
    //    val maxTriangleIdiciesFirst = (correlationPairs(maxTriangle._1)._1, correlationPairs(maxTriangle._2)._1, correlationPairs(maxTriangle._3)._1)
    //    val maxTriangleIdiciesSecond = (correlationPairs(maxTriangle._1)._2, correlationPairs(maxTriangle._2)._2, correlationPairs(maxTriangle._3)._2)
    //
    //    val maxTriangleMatrix: Matrix = GeometryTools.computeTriangleTransformMatrix(first, maxTriangleIdiciesFirst, second, maxTriangleIdiciesSecond)
    //    println(s"Min triangle transform matrix: $maxTriangleMatrix")
    //
    //    val maxSquareTriangleIdiciesFirst = (correlationPairs(maxSquareTriangle._1)._1, correlationPairs(maxSquareTriangle._2)._1, correlationPairs(maxSquareTriangle._3)._1)
    //    val maxSquareTriangleIdiciesSecond = (correlationPairs(maxSquareTriangle._1)._2, correlationPairs(maxSquareTriangle._2)._2, correlationPairs(maxSquareTriangle._3)._2)
    //
    //    val maxSquareTriangleMatrix: Matrix = GeometryTools.computeTriangleTransformMatrix(first, maxSquareTriangleIdiciesFirst, second, maxSquareTriangleIdiciesSecond)
    //    println(s"Max square triangle transform matrix: $maxSquareTriangleMatrix")
    //
    //    applyMatrixToObjFile(new File("data/1HPT_extracted_data", "1HPT_extracted.obj"), new File("data/1HPT_extracted_data", "1HPT_min.obj"), minTriangleMatrix)
    //    applyMatrixToObjFile(new File("data/1HPT_extracted_data", "1HPT_extracted.obj"), new File("data/1HPT_extracted_data", "1HPT_max.obj"), maxTriangleMatrix)
    //    applyMatrixToObjFile(new File("data/1HPT_extracted_data", "1HPT_extracted.obj"), new File("data/1HPT_extracted_data", "1HPT_max_square.obj"), maxSquareTriangleMatrix)

    //        val count = spinImagePairs.par.count({
    //          case ((firstIndex, firstImage), (secondIndex, secondImage)) => (firstImage correlation secondImage) > 0.6
    //        })
    //        println(s"Similarity > 0 $count")
    //
    //        val sortedPoints: Array[(Int, Int, Double)] = correlationPairs.seq.toArray.sortBy(-_._3)
    //        sortedPoints.view take 100 foreach {
    //          case (i, j, c) =>
    //            val firstPoint = first.points(i)
    //            val secondPoint = second.points(j)
    //            println(s"Points $firstPoint, $secondPoint similarity: $c distance: ${firstPoint distance secondPoint}")
    //          //        println(s"First image: \n${firstStack(i)._2}")
    //          //        println(s"Second image: \n${secondStack(j)._2}")
    //        }
    //
    //        sortedPoints.view.zipWithIndex filter {
    //          case ((p1, p2, _), _) => (first.points(p1) distance second.points(p2)) < 1
    //        } take 100 foreach {
    //          case ((p1, p2, c), i) =>
    //            val firstPoint = first.points(p1)
    //            val secondPoint = second.points(p2)
    //            println(s"Points $firstPoint, $secondPoint similarity: $c distance: ${firstPoint distance secondPoint} index: $i")
    //        }

  }


  def findMaxCorrelationPairs(first: Surface, second: Surface): Seq[(Int, Int, Double)] = {
    def spinImageStack(surface: Surface): IndexedSeq[(Int, SpinImage)] = ((0 until surface.points.length).par map {
      i => (i, SpinImage.compute(i, surface, 6.0, 1.0))
    }).toIndexedSeq

    val firstStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(first)
    println(s"Computed first stack ")
    val secondStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(second)
    println(s"Computed second stack ")

    val secondStackGrouped = secondStack.grouped(secondStack.size / 1024).toSeq

    val highCorrelationPairs = secondStackGrouped.par flatMap {
      group: IndexedSeq[(Int, SpinImage)] => {
        val buffer = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Double)]
        for (a <- group; b <- firstStack) {
          val lip = Math.abs(first.lipophilicPotentials(b._1) - second.lipophilicPotentials(a._1))
          val pot = Math.abs(first.electrostaticPotentials(b._1) + second.electrostaticPotentials(a._1))
          val lp: Double = (0.5 - pot / 200.0) + (0.5 - lip / 200.0)
          if (lp > 0.8) {
            val correlation = a._2 correlation b._2
            if (correlation > 0.8) {
              buffer += ((b._1, a._1, correlation + lp))
            }
          }
        }
        buffer
      }
    }

    println(s"Correlation pairs computed. Total size: ${highCorrelationPairs.size}")

    (highCorrelationPairs.seq.sortBy(_._3) reverse) take 50000
  }

  def printClosesPointsSimilarity(first: Surface, second: Surface) {

    def spinImageStack(surface: Surface): IndexedSeq[SpinImage] = ((0 until surface.points.length).par map {
      SpinImage.compute(_, surface, 6.0, 1.0)
    }).toIndexedSeq

    val firstStack: IndexedSeq[SpinImage] = spinImageStack(first)
    println("Computed first stack")
    val secondStack: IndexedSeq[SpinImage] = spinImageStack(second)
    println("Computed second stack")

    val points = for (a <- first.points.indices.iterator; b <- second.points.indices.iterator) yield (a, b, first.points(a) distance second.points(b))

    val pointsWithLip = points.map(x => (Math.abs(first.electrostaticPotentials(x._1) + second.electrostaticPotentials(x._2)), x._3)).toSeq

    println(s"Min lipopholicity potential difference: ${pointsWithLip.minBy(x => x._1)}")
    println(s"Max lipopholicity potential difference: ${pointsWithLip.maxBy(x => x._1)}")
    println(s"Avg lipopholicity potential difference: ${pointsWithLip.foldLeft(0.0)((b, x) => b + x._1) / pointsWithLip.size}")

    val closePointsWithLip = pointsWithLip.filter(x => x._2 < 1.0)

    println(s"Close points Min lipopholicity potential difference: ${closePointsWithLip.minBy(x => x._1)}")
    println(s"Close points Max lipopholicity potential difference: ${closePointsWithLip.maxBy(x => x._1)}")
    println(s"Close points Avg lipopholicity potential difference: ${closePointsWithLip.foldLeft(0.0)((b, x) => b + x._1) / closePointsWithLip.size}")

    val closestPoints = (points filter {
      case (_, _, d) => d < 2
    }).toSeq
    println(s"Closes points computed. Total: ${closestPoints.size}")

    val closesPointsSimilarity = closestPoints.map({
      case (a, b, dist) =>
        (a, b, dist, firstStack(a) correlation secondStack(b))
    })

    val sortedSim = closesPointsSimilarity.sortBy(_._4)

    val maximum = closesPointsSimilarity maxBy (_._4)
    val minimum = closesPointsSimilarity minBy (_._4)
    val average = (closesPointsSimilarity map (_._4) sum) / closesPointsSimilarity.size
    val median = sortedSim(sortedSim.size / 2)._4
    val count = closesPointsSimilarity.count(_._4 > 0.8)

    println(s"Maximum similarity ${maximum._4} for points with distance ${maximum._3}")
    println(s"Minimum similarity ${minimum._4} for points with distance ${minimum._3}")
    println(s"Average similarity $average")
    println(s"Median similarity $median")
    println(s"Similarity > 0.8: $count")

    val maxSimClosesPoints = closesPointsSimilarity filter (_._4 > 0.8)
    println(s"Closes points with sim > 0.8: ${maxSimClosesPoints.size}")

    val firstPointSet: Set[Int] = maxSimClosesPoints.map(_._1).toSet
    val secondPointSet: Set[Int] = maxSimClosesPoints.map(_._2).toSet

    println(s"First surface points count: ${firstPointSet.size}")
    println(s"Second surface points count: ${secondPointSet.size}")


    def isGoodPair(firstIndex: Int, secondIndex: Int) = {
      val firstPair = maxSimClosesPoints(firstIndex)
      val secondPair = maxSimClosesPoints(secondIndex)
      val firstDist = first.points(firstPair._1) distance first.points(secondPair._1)
      val secondDist = second.points(firstPair._2) distance second.points(secondPair._2)
      val delta: Double = Math.abs(firstDist - secondDist)

      val firstDotProduct = first.normals(firstPair._1) dot first.normals(secondPair._1)
      val secondDotProduct = second.normals(firstPair._2) dot second.normals(secondPair._2)

      firstPair._1 != secondPair._1 && firstPair._2 != secondPair._2 && delta < 1 && firstDotProduct > 0 && secondDotProduct > 0
    }

    def maxPointsDist(firstIndex: Int, secondIndex: Int) = {
      val firstPair = maxSimClosesPoints(firstIndex)
      val secondPair = maxSimClosesPoints(secondIndex)
      val firstDist = first.points(firstPair._1) distance first.points(secondPair._1)
      val secondDist = second.points(firstPair._2) distance second.points(secondPair._2)
      Math.max(firstDist, secondDist)
    }

    def delta(firstIndex: Int, secondIndex: Int) = {
      val firstPair = maxSimClosesPoints(firstIndex)
      val secondPair = maxSimClosesPoints(secondIndex)
      val firstDist = first.points(firstPair._1) distance first.points(secondPair._1)
      val secondDist = second.points(firstPair._2) distance second.points(secondPair._2)
      Math.abs(firstDist - secondDist)
    }

    def isGoodTriple(firstIndex: Int, secondIndex: Int, thirdIndex: Int): Boolean = {
      isGoodPair(firstIndex, secondIndex) && isGoodPair(firstIndex, thirdIndex) && isGoodPair(secondIndex, thirdIndex)
    }

    val lines = maxSimClosesPoints.indices.flatMap {
      a: Int => {
        (a + 1 until maxSimClosesPoints.indices.size).filter(isGoodPair(a, _)).map(x => (a, x))
      }
    }

    val maxLine: (Int, Int) = lines.maxBy(x => maxPointsDist(x._1, x._2))
    val minLine: (Int, Int) = lines.minBy(x => maxPointsDist(x._1, x._2))
    val sumLine = lines.foldLeft(0.0)((a, x) => a + maxPointsDist(x._1, x._2))

    println(s"Line with max distance: ${maxPointsDist(maxLine._1, maxLine._2)}")
    println(s"Line with min distance: ${maxPointsDist(minLine._1, minLine._2)}")
    println(s"Avg distance: ${sumLine / lines.size}")

    val maxDelta: (Int, Int) = lines.maxBy(x => delta(x._1, x._2))
    val minDelta: (Int, Int) = lines.minBy(x => delta(x._1, x._2))
    val sumDelta = lines.foldLeft(0.0)((a, x) => a + delta(x._1, x._2))

    println(s"Max delta: ${delta(maxDelta._1, maxDelta._2)}")
    println(s"Min delta: ${delta(minDelta._1, minDelta._2)}")
    println(s"Avg delta: ${sumDelta / lines.size}")


    val counts: Array[Int] = Array.ofDim(maxSimClosesPoints.indices.size)

    lines.foreach {
      case (a, b) =>
        counts(a) += 1
        counts(b) += 1
    }

    println(s"Got ${lines.length} lines")

    println(s"Min lines count for point: ${counts.min}")
    println(s"Max lines count for point: ${counts.max}")
    println(s"Avg lines count for point: ${1.0 * counts.sum / counts.length}")
    println(s"Med lines count for point: ${counts.sorted(Ordering.Int)(counts.length / 2)}")

    val maxPair: Int = counts.indexOf(counts.max)
    println(s"Max pair index: $maxPair")

    val baskets = scala.collection.mutable.ArrayBuffer.empty[ArrayBuffer[(Int, Int)]]

    def fitsToBasket(buffer: ArrayBuffer[(Int, Int)], pair: (Int, Int)): Boolean = {
      buffer.forall(x => lines.contains((x._2, pair._2)) || lines.contains((pair._2, x._2)))
    }

    lines.filter(x => x._1 == maxPair).foreach {
      case (p, x) =>
        val basket: Option[ArrayBuffer[(Int, Int)]] = baskets.find(fitsToBasket(_, (p, x)))
        if (basket.isDefined) {
          basket.get += ((p, x))
        } else {
          val buffer: ArrayBuffer[(Int, Int)] = scala.collection.mutable.ArrayBuffer.empty[(Int, Int)]
          baskets += buffer
          buffer += ((p, x))
        }
    }

    lines.filter(x => x._2 == maxPair).foreach {
      case (x, p) =>
        val basket: Option[ArrayBuffer[(Int, Int)]] = baskets.find(fitsToBasket(_, (p, x)))
        if (basket.isDefined) {
          basket.get += ((p, x))
        } else {
          val buffer: ArrayBuffer[(Int, Int)] = scala.collection.mutable.ArrayBuffer.empty[(Int, Int)]
          baskets += buffer
          buffer += ((p, x))
        }
    }

    println(s"Total baskets count: ${baskets.size}")
    val bs = baskets.foldLeft(0) {
      (x, b) => x + b.size
    }
    println(s"Total elements in baskets: $bs")
    baskets.foreach(b => println(s"Basket: ${b.size}"))

    //    val triangles = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Int)]
    //    for (a <- maxSimClosesPoints.indices; b <- a + 1 until maxSimClosesPoints.size) {
    //      if (isGoodPair(a, b)) {
    //        for (c <- b + 1 until maxSimClosesPoints.size) {
    //          if (isGoodTriple(a, b, c)) {
    //            triangles += ((a, b, c))
    //          }
    //        }
    //      }
    //    }
    //
    //    println(s"Triangles: ${triangles.size}")
    //
    //    def pairDist(pairIndex: Int) = {
    //      val pair = maxSimClosesPoints(pairIndex)
    //      first.points(pair._1) distance second.points(pair._2)
    //    }
    //
    //    val minTriangle = triangles minBy {
    //      case (a, b, c) => pairDist(a) + pairDist(b) + pairDist(c)
    //    }
    //
    //    val maxCorrTriangle = triangles maxBy {
    //      case (a, b, c) => maxSimClosesPoints(a)._4 + maxSimClosesPoints(b)._4 + maxSimClosesPoints(c)._4
    //    }
    //
    //    def triangleLength(triangle: (Int, Int, Int)) = {
    //      val firstPair = maxSimClosesPoints(triangle._1)
    //      val secondPair = maxSimClosesPoints(triangle._2)
    //      val thirdPair = maxSimClosesPoints(triangle._3)
    //
    //      val firstDist = first.points(firstPair._1) distance first.points(secondPair._1)
    //      val secondDist = first.points(secondPair._1) distance first.points(thirdPair._1)
    //      val thirdDist = first.points(firstPair._1) distance first.points(thirdPair._1)
    //      (firstDist, secondDist, thirdDist)
    //    }
    //
    //    println(s"Triangle with min distance has distances: ${pairDist(minTriangle._1)} ${pairDist(minTriangle._2)} ${pairDist(minTriangle._3)}")
    //    println(s"Triangle with min distance has correlations: ${maxSimClosesPoints(minTriangle._1)._4} ${maxSimClosesPoints(minTriangle._2)._4} ${maxSimClosesPoints(minTriangle._3)._4}")
    //    println(s"Triangle with min distance has measures: ${triangleLength(minTriangle)}")
    //    println(s"Triangle with min distance has lipophilicity: [${first.lipophilicPotentials(maxSimClosesPoints(minTriangle._1)._1)} " +
    //      s"${second.lipophilicPotentials(maxSimClosesPoints(minTriangle._1)._2)}] " +
    //      s"[${first.lipophilicPotentials(maxSimClosesPoints(minTriangle._2)._1)} " +
    //      s"${second.lipophilicPotentials(maxSimClosesPoints(minTriangle._2)._2)}] " +
    //      s"[${first.lipophilicPotentials(maxSimClosesPoints(minTriangle._3)._1)} " +
    //      s"${second.lipophilicPotentials(maxSimClosesPoints(minTriangle._3)._2)}]")
    //    println(s"Triangle with min distance has electrostatic: [${first.electrostaticPotentials(maxSimClosesPoints(minTriangle._1)._1)} " +
    //      s"${second.electrostaticPotentials(maxSimClosesPoints(minTriangle._1)._2)}] " +
    //      s"[${first.electrostaticPotentials(maxSimClosesPoints(minTriangle._2)._1)} " +
    //      s"${second.electrostaticPotentials(maxSimClosesPoints(minTriangle._2)._2)}] " +
    //      s"[${first.electrostaticPotentials(maxSimClosesPoints(minTriangle._3)._1)} " +
    //      s"${second.electrostaticPotentials(maxSimClosesPoints(minTriangle._3)._2)}]")
    //    println(s"Triangle with max corr has distances: ${pairDist(maxCorrTriangle._1)} ${pairDist(maxCorrTriangle._2)} ${pairDist(maxCorrTriangle._3)}")
    //    println(s"Triangle with max corr has correlations: ${maxSimClosesPoints(maxCorrTriangle._1)._4} ${maxSimClosesPoints(maxCorrTriangle._2)._4} ${maxSimClosesPoints(maxCorrTriangle._3)._4}")
    //    println(s"Triangle with max corr has measures: ${triangleLength(maxCorrTriangle)}")
    //    println(s"Triangle with max corr has lipophilicity: [${first.lipophilicPotentials(maxSimClosesPoints(maxCorrTriangle._1)._1)} " +
    //      s"${second.lipophilicPotentials(maxSimClosesPoints(maxCorrTriangle._1)._2)}] " +
    //      s"[${first.lipophilicPotentials(maxSimClosesPoints(maxCorrTriangle._2)._1)} " +
    //      s"${second.lipophilicPotentials(maxSimClosesPoints(maxCorrTriangle._2)._2)}] " +
    //      s"[${first.lipophilicPotentials(maxSimClosesPoints(maxCorrTriangle._3)._1)} " +
    //      s"${second.lipophilicPotentials(maxSimClosesPoints(maxCorrTriangle._3)._2)}]")
    //    println(s"Triangle with max corr has electrostatic: [${first.electrostaticPotentials(maxSimClosesPoints(maxCorrTriangle._1)._1)} " +
    //      s"${second.electrostaticPotentials(maxSimClosesPoints(maxCorrTriangle._1)._2)}] " +
    //      s"[${first.electrostaticPotentials(maxSimClosesPoints(maxCorrTriangle._2)._1)} " +
    //      s"${second.electrostaticPotentials(maxSimClosesPoints(maxCorrTriangle._2)._2)}] " +
    //      s"[${first.electrostaticPotentials(maxSimClosesPoints(maxCorrTriangle._3)._1)} " +
    //      s"${first.electrostaticPotentials(maxSimClosesPoints(maxCorrTriangle._3)._2)}]")
    //

    //    closesPoints foreach {
    //      case (a, b, dist) =>
    //        println(s"Points ${first.points(a)} and ${second.points(b)} with distance $dist have similarity ${firstStack(a) similarity secondStack(b)}")
    //        println(s"First image ${firstStack(a)}")
    //        println(s"Second image ${secondStack(b)}")
    //    }

  }

  def printClosesPoints(first: Surface, second: Surface) {
    def distances(): Iterator[(Double, Point, Point)] = for (a <- first.points.iterator; b <- second.points.iterator) yield (a distance b, a, b)
    val minimum: (Double, Point, Point) = distances() minBy (_._1)
    println(s"Minimum distance is $minimum")

    val maximum: (Double, Point, Point) = distances() maxBy (_._1)
    println(s"Maximum distance is $maximum")

    val average: Double = (distances() map (_._1) sum) / (first.points.length * second.points.length)
    println(s"Average distance is $average")

    val points = for (a <- first.points.indices; b <- second.points.indices) yield (a, b, first.points(a) distance second.points(b))
    val closesPoints = points.sortBy(_._3) take 100
    closesPoints foreach {
      case (a, b, dist) =>
        println(s"Points ${first.points(a)} and ${second.points(b)} with distance $dist ")
        println(s"have electrostatic potentials ${first.electrostaticPotentials(a)}, ${second.electrostaticPotentials(b)}")
        println(s"have lipophilic potentials ${first.lipophilicPotentials(a)}, ${second.lipophilicPotentials(b)}")
    }
  }

  def applyMatrixToObjFile(surfaceFile: File, output: File, matrix: Matrix) {
    val lines: Iterator[String] = Source.fromFile(surfaceFile).getLines().map(line => {
      if (line.startsWith("v")) {
        val t: Array[String] = line.split("\\s+")
        val p = new Point(t(1).toDouble, t(2).toDouble, t(3).toDouble)
        val tp: Point = GeometryTools.transformPoint(p, matrix)
        Array(t(0), tp.x, tp.y, tp.z).mkString("", " ", "\n")
      } else {
        line + "\n"
      }
    })
    val writer = new PrintWriter(output)
    lines.foreach(writer.write)
    writer.close()
  }

  def printSurfaceAsPLY(surface: Surface, colors: Seq[(Int, Int, Int)], output: File) {
    val writer = new PrintWriter(output)
    writer.write("ply\n")
    writer.write("format ascii 1.0\n")
    writer.write(s"element vertex ${surface.points.size}\n")
    writer.write("property float x\n")
    writer.write("property float y\n")
    writer.write("property float z\n")
    writer.write("property uchar red\n")
    writer.write("property uchar green\n")
    writer.write("property uchar blue\n")
    writer.write(s"element face ${surface.faces.size}\n")
    writer.write("property list uchar int vertex_index\n")
    writer.write("end_header\n")

    (surface.points zip colors).foreach {
      case (p, (r, g, b)) => writer.write(s"${p.x} ${p.y} ${p.z} $r $g $b\n")
    }

    surface.faces.foreach {
      case (a, b, c) => writer.write(s"3 ${a - 1} ${b - 1} ${c - 1}\n")
    }

    writer.close()
  }

  def printSurfaceAsPLY(first: Surface, second: Surface, correlationPairs: Seq[(Int, Int, Double)], output: File) {
    val writer = new PrintWriter(output)
    writer.write("ply\n")
    writer.write("format ascii 1.0\n")
    writer.write(s"element vertex ${first.points.size + second.points.size}\n")
    writer.write("property float x\n")
    writer.write("property float y\n")
    writer.write("property float z\n")
    writer.write(s"element face ${first.faces.size + second.faces.size}\n")
    writer.write("property list uchar int vertex_index\n")
    writer.write(s"element edge ${correlationPairs.size}\n")
    writer.write("property int vertex1\n")
    writer.write("property int vertex2\n")
    writer.write("end_header\n")

    first.points.foreach(p => writer.write(s"${p.x} ${p.y} ${p.z}\n"))
    second.points.foreach(p => writer.write(s"${p.x} ${p.y} ${p.z}\n"))

    first.faces.foreach {
      case (a, b, c) => writer.write(s"3 ${a - 1} ${b - 1} ${c - 1}\n")
    }

    second.faces.foreach {
      case (a, b, c) => writer.write(s"3 ${a - 1 + first.points.size} ${b - 1 + first.points.size} ${c - 1 + first.points.size}\n")
    }

    //    val delta = correlationPairs.head._3 - correlationPairs.last._3
    //    val dmin = correlationPairs.last._3

    correlationPairs.foreach {
      case (p1, p2, c) =>
        //        val nc = (c - dmin) / delta
        //        val color = Math.round(255.0 * nc)
        writer.write(s"$p1 ${p2 + first.points.size}\n")
    }


    writer.close()
  }


}
