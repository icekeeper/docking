#!/bin/bash
exec scala -savecompiled "$0" "$@"
!#

import java.io.PrintWriter
import scala.collection.mutable
import scala.io.Source

def readSurface(filename: String) = {
  val points = mutable.ArrayBuffer.empty[(Double, Double, Double)]
  val vectors = mutable.ArrayBuffer.empty[(Double, Double, Double)]
  val faces = mutable.ArrayBuffer.empty[(Int, Int, Int)]

  val newPointIndex = mutable.HashMap.empty[(Double, Double, Double), Int]
  val uniquePoints = mutable.HashSet.empty[Int]

  Source.fromFile(filename).getLines() map (_.split(' ')) foreach {
    tokens => tokens(0) match {
      case "v" =>
        val point = (tokens(1).toDouble, tokens(2).toDouble, tokens(3).toDouble)
        if (!newPointIndex.contains(point)) {
          newPointIndex += ((point, newPointIndex.size))
          uniquePoints += points.length
        }
        points += point
      case "vn" => vectors += ((tokens(1).toDouble, tokens(2).toDouble, tokens(3).toDouble))
      case "f" => faces += ((tokens(1).takeWhile(_.isDigit).toInt - 1, tokens(2).takeWhile(_.isDigit).toInt - 1, tokens(3).takeWhile(_.isDigit).toInt - 1))
    }
  }

  println(s"File $filename: ${points.size} points ${vectors.size} vectors and ${faces.size} faces")
  println(s"Points count reduced to ${newPointIndex.size}")
  (points, vectors, faces, newPointIndex, uniquePoints)
}

val surface1 = readSurface(args(0))
val surface2 = readSurface(args(1))

val writer1 = new PrintWriter(args(0))
val writer2 = new PrintWriter(args(1))

val reducedPoints1: Array[(Double, Double, Double)] = surface1._4.toArray.sortBy(_._2).map(_._1)
val reducedPoints2: Array[(Double, Double, Double)] = surface2._4.toArray.sortBy(_._2).map(_._1)

val reducedPoints: Array[(Double, Double, Double)] = reducedPoints1 ++ reducedPoints2

val xc = reducedPoints.map(_._1).sum / reducedPoints.length
val yc = reducedPoints.map(_._2).sum / reducedPoints.length
val zc = reducedPoints.map(_._3).sum / reducedPoints.length


def writeSurface(writer: PrintWriter,
                 reducedPoints: Array[(Double, Double, Double)],
                 points: mutable.ArrayBuffer[(Double, Double, Double)],
                 vectors: mutable.ArrayBuffer[(Double, Double, Double)],
                 faces: mutable.ArrayBuffer[(Int, Int, Int)],
                 newPointIndex: mutable.HashMap[(Double, Double, Double), Int],
                 uniquePoints: mutable.HashSet[Int]) {

  reducedPoints foreach {
    case (x, y, z) => writer.write(s"v ${x - xc} ${y - yc} ${z - zc}\n")
  }

  vectors.zipWithIndex filter {
    case (_, i) => uniquePoints.contains(i)
  } foreach {
    case ((x, y, z), _) => writer.write(s"vn $x $y $z\n")
  }

  faces foreach {
    case (a, b, c) => writer.write(s"f ${newPointIndex(points(a)) + 1} ${newPointIndex(points(b)) + 1} ${newPointIndex(points(c)) + 1}\n")
  }

  writer.close()
}

writeSurface(writer1, reducedPoints1, surface1._1, surface1._2, surface1._3, surface1._4, surface1._5)
writeSurface(writer2, reducedPoints2, surface2._1, surface2._2, surface2._3, surface2._4, surface2._5)






