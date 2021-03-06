package ru.ifmo.main

import java.io.{PrintWriter, File}
import scala.collection.mutable
import scalax.chart.api._
import scala.collection.parallel.ParSeq
import ru.ifmo.docking.model.{SpinImage, Surface}
import scala.collection.JavaConversions._
import ru.ifmo.docking.geometry.{Geometry, Point}
import scala.collection.immutable.Range
import org.jfree.chart.renderer.xy._

object SpinImageTest {

  def main(args: Array[String]) {
    val firstDir = s"data/${args(0)}_data"
    val secondDir = s"data/${args(1)}_data"

    val fiPotentials: File = new File("fi_potentials.txt")
    val first = Surface.read(
      args(0),
      new File(firstDir, s"${args(0)}.obj"),
      new File(firstDir, s"${args(0)}.pdb"),
      new File(firstDir, s"${args(0)}.pqr"),
      fiPotentials
    )

    println(s"First surface read with ${first.points.size()} points")
    val second = Surface.read(
      args(1),
      new File(secondDir, s"${args(1)}.obj"),
      new File(secondDir, s"${args(1)}.pdb"),
      new File(secondDir, s"${args(1)}.pqr"),
      fiPotentials
    )
    println(s"Second surface read with ${second.points.size()} points")

    println(s"First surface resolution ${first.getAverageEdgeLength}")
    println(s"Second surface resolution ${second.getAverageEdgeLength}")

    //                printClosesPoints(first, second)
    //        printClosesPointsSimilarity(first, second)
    //    printMaxSimilarity(first, second)
    //    printPLYFiles(first, second)
    //            printContactPointsStats(first, second)
    //            printSurfaceStats(first, second)
    //    searchDockingSolutions(first, second)
    //            drawGeometryHistogramm(first, second)
    //    drawGeometryDependence(first, second)
    //        testFunction(first, second)
    drawLipHistogram(first, second)
    //        drawElHistogram(first, second)
  }

  def printPLYFiles(first: Surface, second: Surface) {
    val correlationPairs = findMaxCorrelationPairs(first, second, Math.round(first.points.size() * second.points.size() * 0.015).toInt)

    val firstSet = correlationPairs.map(_._1).toSet
    val secondSet = correlationPairs.map(_._2).toSet

    val firstColors: Seq[(Int, Int, Int)] = (0 until first.points.size()).map(i => if (firstSet.contains(i)) (255, 0, 0) else (0, 0, 0))
    val secondColors: Seq[(Int, Int, Int)] = (0 until second.points.size()).map(i => if (secondSet.contains(i)) (255, 0, 0) else (0, 0, 0))

    printSurfaceAsPLY(first, firstColors, new File("first.ply"))
    printSurfaceAsPLY(second, secondColors, new File("second.ply"))
    //    printSurfaceAsPLY(first, second, correlationPairs, new File("combined.ply"))

  }

  def searchDockingSolutions(first: Surface, second: Surface) {
    val startTime = System.currentTimeMillis()
    def time = System.currentTimeMillis() - startTime

    val correlatedPairs = findMaxCorrelationPairs(first, second, pairsCount = 50000)
    println(s"[$time] Correlation bound: ${correlatedPairs.last._3}")

    val firstDiameter = first.getDiameter
    val secondDiameter = second.getDiameter

    val contactSurfaceLimit = Math.max(firstDiameter, secondDiameter) * 0.6
    println(s"[$time] First protein max size: $firstDiameter Second protein max size: $secondDiameter Contact surface size limit: $contactSurfaceLimit")

    def isGoodPair(firstIndex: Int, secondIndex: Int) = {
      val firstPair = correlatedPairs(firstIndex)
      val secondPair = correlatedPairs(secondIndex)
      val firstDist = first.points.get(firstPair._1) distance first.points.get(secondPair._1)
      val secondDist = second.points.get(firstPair._2) distance second.points.get(secondPair._2)
      val delta: Double = Math.abs(firstDist - secondDist)

      (firstPair._1 != secondPair._1
        && firstPair._2 != secondPair._2
        && delta < 1
        && firstDist < contactSurfaceLimit
        && secondDist < contactSurfaceLimit)
    }

    val indices = 0 until correlatedPairs.size

    val linesData = indices.par.map {
      a: Int => {
        indices.filter(x => x != a && isGoodPair(a, x)).toSet
      }
    }.seq

    println(s"[$time] Max lines for point: ${linesData.maxBy(_.size).size}")
    println(s"[$time] Min lines for point: ${linesData.minBy(_.size).size}")
    println(s"[$time] Average lines for point: ${linesData.foldLeft(0.0)((a, s) => a + s.size) / linesData.size}")

    def tomita(r: Set[Int], p: Set[Int], x: Set[Int]): Seq[Set[Int]] = {
      if (p.isEmpty) {
        if (x.isEmpty && r.size >= 3) List(r) else List.empty
      } else {
        val pivot: Int = (p.iterator ++ x.iterator).maxBy(u => (p & linesData(u)).size)
        val candidates = (p -- linesData(pivot)).toSeq
        candidates.par.zipWithIndex.flatMap {
          case (v, i) =>
            val processed = candidates.view.slice(0, i)
            tomita(r + v, (p -- processed) & linesData(v), (x ++ processed) & linesData(v))
        }.seq
      }
    }

    val cliques: Seq[Set[Int]] = tomita(Set.empty, indices.toSet, Set.empty)

    println(s"[$time] Total cliques count: ${cliques.size}")
    println(s"[$time] Max clique size: ${cliques.maxBy(_.size).size}")
    println(s"[$time] Min clique size: ${cliques.minBy(_.size).size}")

    val initialSolutions = cliques.filter(_.forall(x => (first.points.get(correlatedPairs(x)._1) distance second.points.get(correlatedPairs(x)._2)) < 1.0))
    println(s"[$time] Cliquest lead to initial complex count: ${initialSolutions.size}")
    initialSolutions.foreach(c => println(s"[$time] $c"))


  }


  def findMaxCorrelationPairs(first: Surface, second: Surface, pairsCount: Int = 50000): Seq[(Int, Int, Double)] = {
    def smooth(v: Double) = -Math.log(v)
    val maxLipDelta = (for (a <- first.lipophilicity.iterator; b <- second.lipophilicity.iterator) yield smooth(Math.abs(a - b))).max
    val minLipDelta = (for (a <- first.lipophilicity.iterator; b <- second.lipophilicity.iterator) yield smooth(Math.abs(a - b))).min
    println(s"Lip: [$minLipDelta; $maxLipDelta]")
    def lipCorr(a: Int, b: Int): Double = (smooth(Math.abs(first.lipophilicity(a) - second.lipophilicity(b))) - minLipDelta) / (maxLipDelta - minLipDelta)



    val maxElDelta = (for (a <- first.electricity.iterator; b <- second.electricity.iterator) yield smooth(Math.abs(a + b))).max
    val minElDelta = (for (a <- first.electricity.iterator; b <- second.electricity.iterator) yield smooth(Math.abs(a + b))).min
    println(s"El: [$minElDelta; $maxElDelta]")
    def elCorr(a: Int, b: Int): Double = (smooth(Math.abs(first.electricity(a) + second.electricity(b))) - minElDelta) / (maxElDelta - minElDelta)

    def spinImageStack(surface: Surface): IndexedSeq[(Int, SpinImage)] = ((0 until surface.points.size()).par map {
      i => (i, SpinImage.compute(i, surface, 6.0, 1.0))
    }).toIndexedSeq

    val firstStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(first)
    println(s"Computed first stack ")
    val secondStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(second)
    println(s"Computed second stack ")

    val secondStackGrouped = secondStack.grouped(secondStack.size / 8).toSeq

    val pairs = (secondStackGrouped.par flatMap {
      group: IndexedSeq[(Int, SpinImage)] => {
        val buffer = new mutable.PriorityQueue[(Int, Int, Double)]()(Ordering.by(_._3))
        for (a <- group; b <- firstStack) {
          val geo: Double = a._2 correlation b._2
          val lip: Double = lipCorr(b._1, a._1)
          val el: Double = elCorr(b._1, a._1)
          val correlation = lip
          //          if(lip > 0.5 && el > 0.5) {
          if (buffer.length < pairsCount || buffer.head._3 > correlation) {
            buffer += ((b._1, a._1, correlation))
          }
          //          }
          if (buffer.length > pairsCount) {
            buffer.dequeue()
          }
        }
        buffer
      }
    }).seq.sortBy(_._3)

    println(s"Correlation pairs computed. Total size: ${pairs.size}")

    pairs take pairsCount
  }

  def testFunction(first: Surface, second: Surface) {
    def spinImageStack(surface: Surface): IndexedSeq[(Int, SpinImage)] = ((0 until surface.points.size()).par map {
      i => (i, SpinImage.compute(i, surface, 6.0, 1.0))
    }).toIndexedSeq

    val firstStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(first)
    println(s"Computed first stack ")
    val secondStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(second)
    println(s"Computed second stack ")

    val secondStackGrouped = secondStack.grouped((secondStack.size / Runtime.getRuntime.availableProcessors()) + 1).toSeq

    val pointsCount: Int = 1000000
    val highCorrelationPairs = (secondStackGrouped.par flatMap {
      group: IndexedSeq[(Int, SpinImage)] => {
        val buffer = new mutable.PriorityQueue[(Int, Int, Double)]()(Ordering.by(-_._3))
        for (a <- group; b <- firstStack) {
          val correlation: Double = a._2 correlation b._2
          if (buffer.length < pointsCount || buffer.head._3 < correlation) {
            buffer += ((b._1, a._1, correlation))
          }
          if (buffer.length > pointsCount) {
            buffer.dequeue()
          }
        }
        buffer.toList
      }
    }).seq.sortBy(-_._3)

    println(s"First correlation value ${highCorrelationPairs(0)._3}")
    val hcs = highCorrelationPairs.take(pointsCount).map(t => (t._1, t._2)).toSet

    def smooth(v: Double) = -Math.log(v)
    val maxLipDelta = (for (a <- first.lipophilicity.iterator; b <- second.lipophilicity.iterator) yield smooth(Math.abs(a - b))).max
    val minLipDelta = (for (a <- first.lipophilicity.iterator; b <- second.lipophilicity.iterator) yield smooth(Math.abs(a - b))).min
    println(s"Lip: [$minLipDelta; $maxLipDelta]")
    def lipCorr(a: Int, b: Int): Double = (smooth(Math.abs(first.lipophilicity(a) - second.lipophilicity(b))) - minLipDelta) / (maxLipDelta - minLipDelta)

    val highLipPairs = (secondStackGrouped.par flatMap {
      group: IndexedSeq[(Int, SpinImage)] => {
        val buffer = new mutable.PriorityQueue[(Int, Int, Double)]()(Ordering.by(-_._3))
        for (a <- group; b <- firstStack) {
          val geo: Double = a._2 correlation b._2
          val lip: Double = lipCorr(b._1, a._1)
          val correlation = geo + lip
          if (buffer.length < pointsCount || buffer.head._3 < correlation) {
            buffer += ((b._1, a._1, correlation))
          }
          if (buffer.length > pointsCount) {
            buffer.dequeue()
          }
        }
        buffer.toList
      }
    }).seq.sortBy(-_._3)

    println(s"First lip value ${highLipPairs(0)._3}")
    val hls = highLipPairs.take(pointsCount).map(t => (t._1, t._2)).toSet

    val maxElDelta = (for (a <- first.electricity.iterator; b <- second.electricity.iterator) yield smooth(Math.abs(a + b))).max
    val minElDelta = (for (a <- first.electricity.iterator; b <- second.electricity.iterator) yield smooth(Math.abs(a + b))).min
    println(s"El: [$minElDelta; $maxElDelta]")
    def elCorr(a: Int, b: Int): Double = (smooth(Math.abs(first.electricity(a) + second.electricity(b))) - minElDelta) / (maxElDelta - minElDelta)

    val highElPairs = (secondStackGrouped.par flatMap {
      group: IndexedSeq[(Int, SpinImage)] => {
        val buffer = new mutable.PriorityQueue[(Int, Int, Double)]()(Ordering.by(-_._3))
        for (a <- group; b <- firstStack) {
          val geo: Double = a._2 correlation b._2
          val el: Double = elCorr(b._1, a._1)
          val correlation = geo + el

          if (buffer.length < pointsCount || buffer.head._3 < correlation) {
            buffer += ((b._1, a._1, correlation))
          }
          if (buffer.length > pointsCount) {
            buffer.dequeue()
          }
        }
        buffer.toList
      }
    }).seq.sortBy(-_._3)

    println(s"First el value ${highElPairs(0)._3}")
    val hes = highElPairs.take(pointsCount).map(t => (t._1, t._2)).toSet

    val highHzPairs = (secondStackGrouped.par flatMap {
      group: IndexedSeq[(Int, SpinImage)] => {
        val buffer = new mutable.PriorityQueue[(Int, Int, Double)]()(Ordering.by(-_._3))
        for (a <- group; b <- firstStack) {
          val geo: Double = a._2 correlation b._2
          val el: Double = elCorr(b._1, a._1)
          val lip: Double = lipCorr(b._1, a._1)
          val correlation = geo + el + lip

          if (buffer.length < pointsCount || buffer.head._3 < correlation) {
            buffer += ((b._1, a._1, correlation))
          }
          if (buffer.length > pointsCount) {
            buffer.dequeue()
          }
        }
        buffer.toList
      }
    }).seq.sortBy(-_._3)
    val hz = highHzPairs.take(pointsCount).map(t => (t._1, t._2)).toSet


    def closestPointsCount(points: Iterable[(Int, Int)]) = points.count(p => Geometry.distance(first.points.get(p._1), second.points.get(p._2)) < 1.5)

    println(s"Closest points count by spin: ${closestPointsCount(hcs)} ${hcs.size}")
    println(s"Closest points count by spin + lip: ${closestPointsCount(hls)} ${hls.size}")
    println(s"Closest points count by spin + el: ${closestPointsCount(hes)} ${hes.size}")
    println(s"Closest points count by spin + el + lip: ${closestPointsCount(hz)} ${hz.size}")

  }


  def drawGeometryHistogramm(first: Surface, second: Surface) {
    def spinImageStack(surface: Surface): IndexedSeq[(Int, SpinImage)] = ((0 until surface.points.size()).par map {
      i => (i, SpinImage.compute(i, surface, 6.0, 1.0))
    }).toIndexedSeq

    val firstStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(first)
    println(s"Computed first stack ")
    val secondStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(second)
    println(s"Computed second stack ")

    val secondStackGrouped = secondStack.grouped(secondStack.size / Runtime.getRuntime.availableProcessors()).toSeq

    val binCount = 1000
    val binSize = 2.0 / (binCount - 1)

    println(s"Bin count $binCount Bin size $binSize")

    val binsArray = (secondStackGrouped.par map {
      group: IndexedSeq[(Int, SpinImage)] => {
        val bins: Array[Int] = Array.ofDim(binCount)
        for (a <- group;
             b <- firstStack) {
          val correlation = 1 + (a._2 correlation b._2)
          bins(Math.round(correlation / binSize).toInt) += 1
        }
        bins
      }
    }).seq

    println("Spin images computed")

    val bins: Array[Int] = Array.ofDim(binCount)

    binsArray.foreach {
      b => b.zipWithIndex.foreach(p => bins(p._2) += p._1)
    }
    val data = bins.zipWithIndex.map(bin => (binSize * bin._2 - 1, bin._1)).toIterable.toXYSeries()
    val chart = XYLineChart(data, legend = false)
    chart.plot.getDomainAxis.setLabel("значение коэффицента корреляции карт вращения")
    chart.plot.getRangeAxis.setLabel("число пар точек")
    val renderer: XYAreaRenderer = new XYAreaRenderer()
    chart.plot.setRenderer(renderer)
    //    chart.show()
    chart.saveAsPNG("spin_1tgs_hist.png", (600, 400))
  }

  def drawGeometryDependence(first: Surface, second: Surface) {
    def getTopPointsCount(r: Double): Int = {
      println(s"Computing for r = $r")
      def spinImageStack(surface: Surface): IndexedSeq[(Int, SpinImage)] = ((0 until surface.points.size()).par map {
        i => (i, SpinImage.compute(i, surface, r, 1.0))
      }).toIndexedSeq


      val firstStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(first)
      //      println(s"Computed first stack ")
      val secondStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(second)
      //      println(s"Computed second stack ")

      val secondStackGrouped = secondStack.grouped(secondStack.size / Runtime.getRuntime.availableProcessors()).toSeq

      (secondStackGrouped.par flatMap {
        group: IndexedSeq[(Int, SpinImage)] => {
          val buffer = new mutable.PriorityQueue[(Int, Int, Double)]()(Ordering.by(-_._3))
          for (a <- group; b <- firstStack) {
            val correlation: Double = a._2 correlation b._2
            if (buffer.length < 50000 || buffer.head._3 < correlation) {
              buffer += ((b._1, a._1, correlation))
            }
            if (buffer.length > 50000) {
              buffer.dequeue()
            }
          }
          buffer
        }
      }).seq
        .sortBy(-_._3)
        .take(50000)
        .count(p => (first.points.get(p._1) distance second.points.get(p._2)) < 1.0)
    }

    val data = Range.Double.inclusive(5.0, 20.0, 0.1).map(r => (r, getTopPointsCount(r)))

    println(s"Computed data: $data")
    //    val data = List((1, 5.0), (2, 10.0), (3, 2.0), (4, 8.0))
    val chart = XYLineChart(data, legend = false)
    chart.plot.getDomainAxis.setLabel("радиус ограничивающей сферы")
    chart.plot.getRangeAxis.setLabel("число близких пар")
    chart.saveAsPNG("spin_radius2.png", (600, 400))
    //    chart.show(resolution = (600, 400))

  }


  def drawLipHistogram(first: Surface, second: Surface) {
    //    def smooth(v: Double) = -Math.log(v)
    def smooth(v: Double) = v
    val maxDelta = (for (a <- first.lipophilicity.iterator;
                         b <- second.lipophilicity.iterator) yield smooth(Math.abs(a - b))).max
    val minDelta = (for (a <- first.lipophilicity.iterator;
                         b <- second.lipophilicity.iterator) yield smooth(Math.abs(a - b))).min

    println(s"Lipophilicity delta lies in [$minDelta; $maxDelta]")

    //    def corr(a: Double, b: Double): Double = (smooth(Math.abs(a - b)) - minDelta) / (maxDelta - minDelta)
    def corr(a: Double, b: Double): Double = Math.abs(a - b)

    val max = (for (a <- first.lipophilicity.iterator;
                    b <- second.lipophilicity.iterator) yield corr(a, b)).max
    val min = (for (a <- first.lipophilicity.iterator;
                    b <- second.lipophilicity.iterator) yield corr(a, b)).min

    println(s"Function from lip lies in [$min; $max]")

    val binCount = 100000
    val binSize = max / (binCount - 1)

    println(s"Bin count $binCount Bin size $binSize")

    val bins: Array[Int] = Array.ofDim(binCount)

    for (a <- first.lipophilicity.iterator;
         b <- second.lipophilicity.iterator) {
      bins(Math.round(corr(a, b) / binSize).toInt) += 1
    }

    val data = bins.zipWithIndex.map(bin => (bin._2 * binSize, bin._1)).toIterable

    val chart = XYLineChart(data, legend = false)
    chart.plot.getDomainAxis.setLabel("нормированный модуль разности потенциалов")
    chart.plot.getRangeAxis.setLabel("число пар точек")
    val renderer: XYAreaRenderer = new XYAreaRenderer()
    chart.plot.setRenderer(renderer)
    chart.show()
    //    chart.saveAsPNG("lip_1tgs_hist_norm.png", (600, 400))
  }

  def drawElHistogram(first: Surface, second: Surface) {
    def smooth(v: Double) = -Math.log(v)
    //    def smooth(v: Double) = v
    val maxDelta = (for (a <- first.electricity.iterator;
                         b <- second.electricity.iterator) yield smooth(Math.abs(a + b))).max
    val minDelta = (for (a <- first.electricity.iterator;
                         b <- second.electricity.iterator) yield smooth(Math.abs(a + b))).min

    println(s"Electricity delta lies in [$minDelta; $maxDelta]")


    def corr(a: Double, b: Double): Double = (smooth(Math.abs(a + b)) - minDelta) / (maxDelta - minDelta)
    //    def corr(a: Double, b: Double): Double = Math.abs(a + b)

    val max = (for (a <- first.electricity.iterator;
                    b <- second.electricity.iterator) yield corr(a, b)).max
    val min = (for (a <- first.electricity.iterator;
                    b <- second.electricity.iterator) yield corr(a, b)).min
    val binCount = 100000
    val binSize = max / (binCount - 1)

    println(s"Function from el lies in [$min; $max]")
    println(s"Bin count $binCount Bin size $binSize")

    val bins: Array[Int] = Array.ofDim(binCount)

    for (a <- first.electricity.iterator;
         b <- second.electricity.iterator) {
      bins(Math.max(Math.round(corr(a, b) / binSize).toInt, 0)) += 1
    }

    val data = bins.zipWithIndex.map(bin => (bin._2 * binSize, bin._1)).toIterable

    val chart = XYLineChart(data, legend = false)
    chart.plot.getDomainAxis.setLabel("нормированная сумма потенциалов")
    chart.plot.getRangeAxis.setLabel("число пар точек")
    val renderer: XYAreaRenderer = new XYAreaRenderer()
    chart.plot.setRenderer(renderer)
    //        chart.show()
    chart.saveAsPNG("el_1tgs_hist_norm.png", (600, 400))
  }

  def printSurfaceStats(first: Surface, second: Surface) {
    println(s"Average edge length for a first surface is ${
      first.getAverageEdgeLength
    }")
    println(s"Average edge length for a second surface is ${
      second.getAverageEdgeLength
    }")

    val firstPoints = for (a <- first.points.indices.iterator;
                           b <- first.points.indices.iterator) yield first.points(a) distance first.points(b)
    println(s"Diameter of first protein: ${
      firstPoints.max
    }")

    val secondPoints = for (a <- second.points.indices.iterator;
                            b <- second.points.indices.iterator) yield second.points(a) distance second.points(b)
    println(s"Diameter of second protein: ${
      secondPoints.max
    }")

    val points = for (a <- first.points.indices.iterator;
                      b <- second.points.indices.iterator) yield (a, b, first.points(a) distance second.points(b))

    val closePairs = points.filter(_._3 < 1.0).toSeq

    val pairPairs = for (a <- closePairs;
                         b <- closePairs) yield Math.max(first.points(a._1) distance first.points(b._1), second.points(a._2) distance second.points(b._2))
    println(s"Contact surface diameter ${
      pairPairs.max
    }")

    val maxLipDiff = (for (a <- first.lipophilicity.iterator;
                           b <- second.lipophilicity.iterator) yield Math.abs(a - b)).max
    val maxElDiff = (for (a <- first.electricity.iterator;
                          b <- second.electricity.iterator) yield Math.abs(a + b)).max

    println(s"Max lip diff $maxLipDiff")
    println(s"Max el diff $maxElDiff")

    val pairsCount: Int = first.points.length * second.points.length

    val averageLip = (for (a <- first.lipophilicity.iterator;
                           b <- second.lipophilicity.iterator) yield Math.abs(a - b) / maxLipDiff).sum / pairsCount
    println(s"Average lip correlation: $averageLip")

    val medianLip = (for (a <- first.lipophilicity;
                          b <- second.lipophilicity) yield Math.abs(a - b) / maxLipDiff).sorted
    println(s"Median lip correlation: ${
      medianLip(medianLip.size / 2)
    }")

    val averageEl = (for (a <- first.electricity.iterator;
                          b <- second.electricity.iterator) yield Math.abs(a + b) / maxElDiff).sum / pairsCount
    println(s"Average el correlation: $averageEl")

    val medianEl = (for (a <- first.electricity;
                         b <- second.electricity) yield Math.abs(a + b) / maxElDiff).sorted
    println(s"Median el correlation: ${
      medianEl(medianEl.size / 2)
    }")
  }

  def printContactPointsStats(first: Surface, second: Surface) {
    val spinImageBound: Double = 6.0
    println(s"Spin image bound $spinImageBound")

    def spinImageStack(surface: Surface): IndexedSeq[(Int, SpinImage)] = ((0 until surface.points.length).par map {
      i => (i, SpinImage.compute(i, surface, spinImageBound, 1.0))
    }).toIndexedSeq

    val firstStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(first)
    println(s"Computed first stack ")
    val secondStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(second)
    println(s"Computed second stack ")

    val secondStackGrouped = secondStack.grouped(secondStack.size / 1024).toSeq

    val maxLipDiff = (for (a <- first.lipophilicity.iterator;
                           b <- second.lipophilicity.iterator) yield Math.abs(a - b)).max
    val maxElDiff = (for (a <- first.electricity.iterator;
                          b <- second.electricity.iterator) yield Math.abs(a + b)).max

    val minLipDiff = (for (a <- first.lipophilicity.iterator;
                           b <- second.lipophilicity.iterator) yield Math.abs(a - b)).min
    val minElDiff = (for (a <- first.electricity.iterator;
                          b <- second.electricity.iterator) yield Math.abs(a + b)).min

    println(s"Common lipophilicity delta is in [${
      minLipDiff / maxLipDiff
    }; ${
      maxLipDiff / maxLipDiff
    }]")
    println(s"Common electricity delta is in [${
      minElDiff / maxElDiff
    }; ${
      maxElDiff / maxElDiff
    }]")

    val pairsCount: Int = first.points.length * second.points.length
    val averageLip = (for (a <- first.lipophilicity.iterator;
                           b <- second.lipophilicity.iterator) yield Math.abs(a - b)).sum / pairsCount
    val averageEl = (for (a <- first.electricity.iterator;
                          b <- second.electricity.iterator) yield Math.abs(a + b)).sum / pairsCount

    println(s"Average lipophilicity delta is $averageLip")
    println(s"Average electricity delta is $averageEl")

    val data = secondStackGrouped.par flatMap {
      group: IndexedSeq[(Int, SpinImage)] => {
        val buffer = mutable.ArrayBuffer.empty[(Double, Double, Double, Double)]
        for (a <- group;
             b <- firstStack) {
          val lip = 1 - Math.abs(first.lipophilicity(b._1) - second.lipophilicity(a._1)) / maxLipDiff
          val pot = 1 - Math.abs(first.electricity(b._1) + second.electricity(a._1)) / maxElDiff
          val correlation = a._2 correlation b._2
          val distance = first.points(b._1) distance second.points(a._1)

          if (distance < 1.0 || correlation > 0.8) {
            buffer += ((distance, lip, pot, correlation))
          }
        }
        buffer
      }
    }

    println(s"Total points count ${
      data.size
    }")

    println(s"Minimum distance ${
      data.minBy(_._1)
    }")

    val closestPoints: ParSeq[(Double, Double, Double, Double)] = data.filter(_._1 < 1.0)

    println(s"Closest points count with dist < 1: ${
      closestPoints.size
    }")

    val closeMaxLipDelta = closestPoints.map(_._2).max
    val closeMaxElDelta = closestPoints.map(_._3).max

    val closeMinLipDelta = closestPoints.map(_._2).min
    val closeMinElDelta = closestPoints.map(_._3).min

    println(s"Contact points lipophilicity delta is in [$closeMinLipDelta; $closeMaxLipDelta]")
    println(s"Contact points electricity delta is in [$closeMinElDelta; $closeMaxElDelta]")

    val closestAverageLip: Double = closestPoints.map(_._2).sum / closestPoints.length
    val closestAverageEl: Double = closestPoints.map(_._3).sum / closestPoints.length

    println(s"Average contact points lipophilicity delta is $closestAverageLip")
    println(s"Average contact points electricity delta is $closestAverageEl")


    def complexCorrelation(data: (Double, Double, Double, Double)): Double = {
      Math.max(Math.max(data._2, data._3), data._4)
    }

    val sortedByCorrelation: Seq[(Double, Double, Double, Double)] = data.seq.sortBy(-complexCorrelation(_))
    val topCandidates: Seq[((Double, Double, Double, Double), Int)] = sortedByCorrelation.zipWithIndex.filter(p => p._1._1 < 1.0).take(1000)
    topCandidates.foreach(p => println(s"Position ${
      p._2
    } data ${
      p._1
    } correlation ${
      complexCorrelation(p._1)
    }"))

  }

  def printClosesPointsSimilarity(first: Surface, second: Surface) {

    def spinImageStack(surface: Surface): IndexedSeq[SpinImage] = ((0 until surface.points.length).par map {
      SpinImage.compute(_, surface, 6.0, 1.0)
    }).toIndexedSeq

    val firstStack: IndexedSeq[SpinImage] = spinImageStack(first)
    println("Computed first stack")
    val secondStack: IndexedSeq[SpinImage] = spinImageStack(second)
    println("Computed second stack")

    val points = for (a <- first.points.indices.iterator;
                      b <- second.points.indices.iterator) yield (a, b, first.points(a) distance second.points(b))

    val pointsWithLip = points.map(x => (Math.abs(first.electricity(x._1) + second.electricity(x._2)), x._3)).toSeq

    println(s"Min lipopholicity potential difference: ${
      pointsWithLip.minBy(x => x._1)
    }")
    println(s"Max lipopholicity potential difference: ${
      pointsWithLip.maxBy(x => x._1)
    }")
    println(s"Avg lipopholicity potential difference: ${
      pointsWithLip.foldLeft(0.0)((b, x) => b + x._1) / pointsWithLip.size
    }")

    val closePointsWithLip = pointsWithLip.filter(x => x._2 < 1.0)

    println(s"Close points Min lipopholicity potential difference: ${
      closePointsWithLip.minBy(x => x._1)
    }")
    println(s"Close points Max lipopholicity potential difference: ${
      closePointsWithLip.maxBy(x => x._1)
    }")
    println(s"Close points Avg lipopholicity potential difference: ${
      closePointsWithLip.foldLeft(0.0)((b, x) => b + x._1) / closePointsWithLip.size
    }")

    val closestPoints = (points filter {
      case (_, _, d) => d < 2
    }).toSeq
    println(s"Closes points computed. Total: ${
      closestPoints.size
    }")

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

    println(s"Maximum similarity ${
      maximum._4
    } for points with distance ${
      maximum._3
    }")
    println(s"Minimum similarity ${
      minimum._4
    } for points with distance ${
      minimum._3
    }")
    println(s"Average similarity $average")
    println(s"Median similarity $median")
    println(s"Similarity > 0.8: $count")

    val maxSimClosesPoints = closesPointsSimilarity filter (_._4 > 0.8)
    println(s"Closes points with sim > 0.8: ${
      maxSimClosesPoints.size
    }")

    val firstPointSet: Set[Int] = maxSimClosesPoints.map(_._1).toSet
    val secondPointSet: Set[Int] = maxSimClosesPoints.map(_._2).toSet

    println(s"First surface points count: ${
      firstPointSet.size
    }")
    println(s"Second surface points count: ${
      secondPointSet.size
    }")


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

    val lines = maxSimClosesPoints.indices.flatMap {
      a: Int => {
        (a + 1 until maxSimClosesPoints.indices.size).filter(isGoodPair(a, _)).map(x => (a, x))
      }
    }

    val maxLine: (Int, Int) = lines.maxBy(x => maxPointsDist(x._1, x._2))
    val minLine: (Int, Int) = lines.minBy(x => maxPointsDist(x._1, x._2))
    val sumLine = lines.foldLeft(0.0)((a, x) => a + maxPointsDist(x._1, x._2))

    println(s"Line with max distance: ${
      maxPointsDist(maxLine._1, maxLine._2)
    }")
    println(s"Line with min distance: ${
      maxPointsDist(minLine._1, minLine._2)
    }")
    println(s"Avg distance: ${
      sumLine / lines.size
    }")

    val maxDelta: (Int, Int) = lines.maxBy(x => delta(x._1, x._2))
    val minDelta: (Int, Int) = lines.minBy(x => delta(x._1, x._2))
    val sumDelta = lines.foldLeft(0.0)((a, x) => a + delta(x._1, x._2))

    println(s"Max delta: ${
      delta(maxDelta._1, maxDelta._2)
    }")
    println(s"Min delta: ${
      delta(minDelta._1, minDelta._2)
    }")
    println(s"Avg delta: ${
      sumDelta / lines.size
    }")


    val counts: Array[Int] = Array.ofDim(maxSimClosesPoints.indices.size)

    lines.foreach {
      case (a, b) =>
        counts(a) += 1
        counts(b) += 1
    }

    println(s"Got ${
      lines.length
    } lines")

    println(s"Min lines count for point: ${
      counts.min
    }")
    println(s"Max lines count for point: ${
      counts.max
    }")
    println(s"Avg lines count for point: ${
      1.0 * counts.sum / counts.length
    }")
    println(s"Med lines count for point: ${
      counts.sorted(Ordering.Int)(counts.length / 2)
    }")

    val maxPair: Int = counts.indexOf(counts.max)
    println(s"Max pair index: $maxPair")

    val baskets = mutable.ArrayBuffer.empty[mutable.ArrayBuffer[(Int, Int)]]

    def fitsToBasket(buffer: mutable.ArrayBuffer[(Int, Int)], pair: (Int, Int)): Boolean = {
      buffer.forall(x => lines.contains((x._2, pair._2)) || lines.contains((pair._2, x._2)))
    }

    lines.filter(x => x._1 == maxPair).foreach {
      case (p, x) =>
        val basket: Option[mutable.ArrayBuffer[(Int, Int)]] = baskets.find(fitsToBasket(_, (p, x)))
        if (basket.isDefined) {
          basket.get += ((p, x))
        } else {
          val buffer: mutable.ArrayBuffer[(Int, Int)] = mutable.ArrayBuffer.empty[(Int, Int)]
          baskets += buffer
          buffer += ((p, x))
        }
    }

    lines.filter(x => x._2 == maxPair).foreach {
      case (x, p) =>
        val basket: Option[mutable.ArrayBuffer[(Int, Int)]] = baskets.find(fitsToBasket(_, (p, x)))
        if (basket.isDefined) {
          basket.get += ((p, x))
        } else {
          val buffer: mutable.ArrayBuffer[(Int, Int)] = mutable.ArrayBuffer.empty[(Int, Int)]
          baskets += buffer
          buffer += ((p, x))
        }
    }

    println(s"Total baskets count: ${
      baskets.size
    }")
    val bs = baskets.foldLeft(0) {
      (x, b) => x + b.size
    }
    println(s"Total elements in baskets: $bs")
    baskets.foreach(b => println(s"Basket: ${
      b.size
    }"))

  }

  def printClosesPoints(first: Surface, second: Surface) {
    def distances(): Iterator[(Double, Point, Point)] = for (a <- first.points.iterator;
                                                             b <- second.points.iterator) yield (a distance b, a, b)
    val minimum: (Double, Point, Point) = distances() minBy (_._1)
    println(s"Minimum distance is $minimum")

    val maximum: (Double, Point, Point) = distances() maxBy (_._1)
    println(s"Maximum distance is $maximum")

    val average: Double = (distances() map (_._1) sum) / (first.points.length * second.points.length)
    println(s"Average distance is $average")

    val points = for (a <- first.points.indices;
                      b <- second.points.indices) yield (a, b, first.points(a) distance second.points(b))
    val closesPoints = points.sortBy(_._3) take 100
    closesPoints foreach {
      case (a, b, dist) =>
        println(s"Points ${
          first.points(a)
        } and ${
          second.points(b)
        } with distance $dist ")
        println(s"have electrostatic potentials ${
          first.electricity(a)
        }, ${
          second.electricity(b)
        }")
        println(s"have lipophilic potentials ${
          first.lipophilicity(a)
        }, ${
          second.lipophilicity(b)
        }")
    }
  }

  def printSurfaceAsPLY(surface: Surface, colors: Seq[(Int, Int, Int)], output: File) {
    val writer = new PrintWriter(output)
    writer.write("ply\n")
    writer.write("format ascii 1.0\n")
    writer.write(s"element vertex ${
      surface.points.size
    }\n")
    writer.write("property float x\n")
    writer.write("property float y\n")
    writer.write("property float z\n")
    writer.write("property uchar red\n")
    writer.write("property uchar green\n")
    writer.write("property uchar blue\n")
    writer.write(s"element face ${
      surface.faces.size
    }\n")
    writer.write("property list uchar int vertex_index\n")
    writer.write("end_header\n")

    (surface.points zip colors).foreach {
      case (p, (r, g, b)) => writer.write(s"${
        p.x
      } ${
        p.y
      } ${
        p.z
      } $r $g $b\n")
    }

    surface.faces.foreach {
      f: Surface.Face => writer.write(s"3 ${
        f.p1 - 1
      } ${
        f.p2 - 1
      } ${
        f.p3 - 1
      }\n")
    }

    writer.close()
  }

  def printSurfaceAsPLY(first: Surface, second: Surface, correlationPairs: Seq[(Int, Int, Double)], output: File) {
    val writer = new PrintWriter(output)
    writer.write("ply\n")
    writer.write("format ascii 1.0\n")
    writer.write(s"element vertex ${
      first.points.size + second.points.size
    }\n")
    writer.write("property float x\n")
    writer.write("property float y\n")
    writer.write("property float z\n")
    writer.write(s"element face ${
      first.faces.size + second.faces.size
    }\n")
    writer.write("property list uchar int vertex_index\n")
    writer.write(s"element edge ${
      correlationPairs.size
    }\n")
    writer.write("property int vertex1\n")
    writer.write("property int vertex2\n")
    writer.write("end_header\n")

    first.points.foreach(p => writer.write(s"${
      p.x
    } ${
      p.y
    } ${
      p.z
    }\n"))
    second.points.foreach(p => writer.write(s"${
      p.x
    } ${
      p.y
    } ${
      p.z
    }\n"))

    first.faces.foreach {
      f: Surface.Face => writer.write(s"3 ${
        f.p1 - 1
      } ${
        f.p2 - 1
      } ${
        f.p3 - 1
      }\n")
    }

    second.faces.foreach {
      f: Surface.Face => writer.write(s"3 ${
        f.p1 - 1 + first.points.size
      } ${
        f.p2 - 1 + first.points.size
      } ${
        f.p3 - 1 + first.points.size
      }\n")
    }

    //    val delta = correlationPairs.head._3 - correlationPairs.last._3
    //    val dmin = correlationPairs.last._3

    correlationPairs.foreach {
      case (p1, p2, c) =>
        //        val nc = (c - dmin) / delta
        //        val color = Math.round(255.0 * nc)
        writer.write(s"$p1 ${
          p2 + first.points.size
        }\n")
    }


    writer.close()
  }


}
