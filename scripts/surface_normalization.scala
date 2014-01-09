#!/bin/bash
exec scala -savecompiled "$0" "$@"
!#

import java.io.PrintWriter
import scala.collection.mutable
import scala.io.Source

val points = mutable.ArrayBuffer.empty[(Double, Double, Double)]
val vectors = mutable.ArrayBuffer.empty[(Double, Double, Double)]
val surfaces = mutable.ArrayBuffer.empty[(Int, Int, Int)]

val newPointIndex = mutable.HashMap.empty[(Double, Double, Double), Int]
val uniquePoints = mutable.HashSet.empty[Int]

Source.fromFile(args(0)).getLines() map (_.split(' ')) foreach {
  tokens => tokens(0) match {
    case "v" =>
      val point = (tokens(1).toDouble, tokens(2).toDouble, tokens(3).toDouble)
      if (!newPointIndex.contains(point)) {
        newPointIndex += ((point, newPointIndex.size))
        uniquePoints += points.length
      }
      points += point
    case "vn" => vectors += ((tokens(1).toDouble, tokens(2).toDouble, tokens(3).toDouble))
    case "f" => surfaces += ((tokens(1).takeWhile(_.isDigit).toInt - 1, tokens(2).takeWhile(_.isDigit).toInt - 1, tokens(3).takeWhile(_.isDigit).toInt - 1))
  }
}

println(s"Read ${points.size} points ${vectors.size} vectors and ${surfaces.size} faces")
println(s"Points count reduced to ${newPointIndex.size}")

val writer = new PrintWriter(args(1))

val reducedPoints: Array[(Double, Double, Double)] = newPointIndex.toArray.sortBy(_._2).map(_._1)

reducedPoints foreach {
  case (x, y, z) => writer.write(s"v $x $y $z\n")
}

vectors.zipWithIndex filter {
  case (_, i) => uniquePoints.contains(i)
} foreach {
  case ((x, y, z), _) => writer.write(s"vn $x $y $z\n")
}

surfaces foreach {
  case (a, b, c) => writer.write(s"f ${newPointIndex(points(a)) + 1} ${newPointIndex(points(b)) + 1} ${newPointIndex(points(c)) + 1}\n")
}

writer.close()


