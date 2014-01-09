package ru.ifmo.main

import java.io.File
import ru.ifmo.model.{SpinImage, Surface, Point}

object SpinImageTest {

  def main(args: Array[String]) {
    val firstDir = s"data/${args(0)}_data"
    val secondDir = s"data/${args(1)}_data"

    val first = Surface.read(new File(firstDir, s"${args(0)}.obj"), new File(firstDir, s"${args(0)}_pot.csv"), new File(firstDir, s"${args(0)}_lip.csv"))
    val second = Surface.read(new File(secondDir, s"${args(1)}.obj"), new File(secondDir, s"${args(1)}_pot.csv"), new File(secondDir, s"${args(1)}_lip.csv"))

    printClosesPoints(first, second)
    //    printClosesPointsSimilarity(first, second)
    printMaxSimilarity(first, second)
  }

  def printMaxSimilarity(first: Surface, second: Surface) {

    def spinImageStack(surface: Surface): IndexedSeq[(Int, SpinImage)] = ((0 until surface.points.length).par map {
      i => (i, SpinImage.compute(i, surface, 6.0, 1.0))
    }).toIndexedSeq

    val firstStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(first)
    println("Computed first stack")
    val secondStack: IndexedSeq[(Int, SpinImage)] = spinImageStack(second)
    println("Computed second stack")

    val spinImagePairs = for (a <- firstStack; b <- secondStack) yield (a, b)
    println("Spin image pairs generated")

    val correlationPairs = spinImagePairs.par map {
      case ((firstIndex, firstImage), (secondIndex, secondImage)) => (firstIndex, secondIndex, firstImage similarity secondImage)
    }

    println("Spin image stack computed")

    val sortedPoints: Array[(Int, Int, Double)] = correlationPairs.seq.toArray.sortBy(-_._3)
    sortedPoints.view take 100 foreach {
      case (i, j, c) =>
        val firstPoint = first.points(i)
        val secondPoint = second.points(j)
        println(s"Points $firstPoint, $secondPoint similarity: $c distance: ${firstPoint distance secondPoint}")
      //        println(s"First image: \n${firstStack(i)._2}")
      //        println(s"Second image: \n${secondStack(j)._2}")
    }

    sortedPoints.view.zipWithIndex filter {
      case ((p1, p2, _), _) => (first.points(p1) distance second.points(p2)) < 1
    } take 100 foreach {
      case ((p1, p2, c), i) =>
        val firstPoint = first.points(p1)
        val secondPoint = second.points(p2)
        println(s"Points $firstPoint, $secondPoint similarity: $c distance: ${firstPoint distance secondPoint} index: $i")
    }

  }

  def printClosesPointsSimilarity(first: Surface, second: Surface) {

    def spinImageStack(surface: Surface): IndexedSeq[SpinImage] = ((0 until surface.points.length).par map {
      SpinImage.compute(_, surface, 2.0, 0.6)
    }).toIndexedSeq

    val firstStack: IndexedSeq[SpinImage] = spinImageStack(first)
    println("Computed first stack")
    val secondStack: IndexedSeq[SpinImage] = spinImageStack(second)
    println("Computed second stack")

    val points = for (a <- first.points.indices; b <- second.points.indices) yield (a, b, first.points(a) distance second.points(b))
    val closesPoints = points.sortBy(_._3) take 100
    println("Closes points computed")

    closesPoints foreach {
      case (a, b, dist) =>
        println(s"Points ${first.points(a)} and ${second.points(b)} with distance $dist have similarity ${firstStack(a) similarity secondStack(b)}")
        println(s"First image ${firstStack(a)}")
        println(s"Second image ${secondStack(b)}")
    }

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
}
