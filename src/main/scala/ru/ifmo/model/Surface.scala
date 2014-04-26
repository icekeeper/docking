package ru.ifmo.model

import java.io.File
import scala.io.Source
import ru.ifmo.docking.lip.LipophilicityCalculator
import ru.ifmo.docking.geometry

class Surface(
               pc: Array[Point],
               nc: Array[Vector],
               sc: Array[(Int, Int, Int)],
               ec: Array[Double],
               lc: Array[Double]
               ) {

  val points: Array[Point] = pc
  val normals: Array[Vector] = nc
  val electrostaticPotentials: Array[Double] = ec
  val lipophilicPotentials: Array[Double] = lc
  val faces: Array[(Int, Int, Int)] = sc

  lazy val meshResolution: Double = {
    val edgesLengthSum = faces.foldLeft(0.0) {
      case (acc, (a, b, c)) => acc + (points(a) distance points(b)) + (points(b) distance points(c)) + (points(a) distance points(c))
    }
    edgesLengthSum / ((faces.length + points.length - 2) * 2) //euler formula for edges count
  }

  def transform(matrix: Matrix): Surface = {
    val transformedPoints = points.map(p => GeometryTools.transformPoint(p, matrix))
    val transformedNormals = normals.map(n => GeometryTools.transformVector(n, matrix))
    new Surface(transformedPoints, transformedNormals, faces, electrostaticPotentials, lipophilicPotentials)
  }

}

object Surface {

  def read(surfaceFile: File, electrostaticFile: File, fiPotentials: File, pdbFile: File): Surface = {
    val points: Array[Point] = readPoints(surfaceFile)
    println(s"Read ${points.length} points")
    new Surface(
      points,
      readNormals(surfaceFile),
      readSurfaces(surfaceFile),
      readCsv(electrostaticFile),
      computeLipophilicity(points, fiPotentials, pdbFile)
    )
  }

  private def computeLipophilicity(points: Array[Point], fiPotentials: File, pdbFile: File): Array[Double] = {
    val calculator: LipophilicityCalculator = LipophilicityCalculator.construct(pdbFile, fiPotentials)
    points.map(p => calculator.compute(new geometry.Point(p.x, p.y, p.z)))
  }

  private def readPoints(surfaceFile: File): Array[Point] = {
    val lines: Iterator[String] = Source.fromFile(surfaceFile).getLines().filter(s => s.startsWith("v "))
    val tokens: Iterator[Array[String]] = lines map (_.split("\\s+"))
    val points: Iterator[Point] = tokens map (t => new Point(t(1).toDouble, t(2).toDouble, t(3).toDouble))
    points.toArray
  }

  private def readNormals(surfaceFile: File): Array[Vector] = {
    val lines: Iterator[String] = Source.fromFile(surfaceFile).getLines().filter(s => s.startsWith("vn "))
    val tokens: Iterator[Array[String]] = lines map (_.split("\\s+"))
    val vectors: Iterator[Vector] = tokens map (t => new Vector(t(1).toDouble, t(2).toDouble, t(3).toDouble))
    val normals: Iterator[Vector] = vectors map (_.unite)
    normals.toArray
  }

  private def readSurfaces(surfaceFile: File): Array[(Int, Int, Int)] = {
    val lines: Iterator[String] = Source.fromFile(surfaceFile).getLines().filter(s => s.startsWith("f "))
    val tokens: Iterator[Array[String]] = lines map (_.split("\\s+"))
    val surfaces: Iterator[(Int, Int, Int)] = tokens map (t => (t(1).takeWhile(_.isDigit).toInt, t(2).takeWhile(_.isDigit).toInt, t(3).takeWhile(_.isDigit).toInt))
    surfaces.toArray
  }

  private def readCsv(surfaceFile: File): Array[Double] = {
    val lines: Iterator[String] = Source.fromFile(surfaceFile).getLines()
    val values: Iterator[Double] = lines map (s => s.substring(s.lastIndexOf(',') + 1).toDouble)
    values.toArray
  }
}


