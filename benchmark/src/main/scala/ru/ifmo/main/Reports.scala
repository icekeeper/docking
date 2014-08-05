package ru.ifmo.main

import scala.io.Source
import java.io.{FilenameFilter, File}
import scala.collection.immutable.Iterable
import scalax.chart.api._
import java.awt.BasicStroke

object Reports {

  def readEval(f: File, docker: String): Map[String, List[Double]] = {
    val files = f.list(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.startsWith(docker)
    })

    files.map(file => {
      println(s"read $file")
      (file.substring(file.indexOf('_') + 1, file.indexOf('_') + 5),
        Source
          .fromFile(new File(f, file))
          .getLines()
          //          .take(100000)
          .map(line => line.split('\t')(1).toDouble)
          .toList
        )
    }).filter(p => p._2.exists(_ > 2.5))
      .toMap
  }

  def getSuccessPoints(m: Map[String, List[Double]], keys: Set[String], prec: Double, count: Int): Seq[(Int, Int)] = {
    val firstHits: Iterable[Int] = m
      .filter(p => keys.contains(p._1))
      .map(p => p._2.zipWithIndex.find(_._1 < prec).getOrElse((0, 200000))._2 + 1)
      .filter(_ <= count)

    Range.inclusive(1, count).map(i => (i, firstHits.count(_ <= i)))
  }

  def countHits(m: Map[String, List[Double]], keys: Set[String], prec: Double, count: Int): Int = {
    m.filter(p => keys.contains(p._1)).count(p => p._2.take(count).exists(_ < prec))
  }

  def countSuccess(zdock: Map[String, List[Double]], geo: Map[String, List[Double]]): Int = {
    val keys: Iterable[String] = zdock.filter(p => p._2.forall(_ > 2.5)).map(_._1)
    keys.filter(geo.contains).count(key => geo(key).take(50000).exists(_ < 2.5))
  }

  def main(args: Array[String]) {
    val dir = new File("/Users/vikharev/Documents/run2/evaluated")

    val zdock = readEval(dir, "zdock")
    val geo = readEval(dir, "geometry")
    val lip = readEval(dir, "lipophilic")
    val elc = readEval(dir, "electric")

    println(s"zdock count: ${zdock.size} geo count: ${geo.size} lip count: ${lip.size} elc count: ${elc.size}")

    def printHits(m: Map[String, List[Double]], keys: Set[String], name: String) {
      println(s"2k $name 2.5 hits: ${countHits(m, keys, 2.5, 2000)}")
      println(s"50k $name 2.5 hits: ${countHits(m, keys, 2.5, 50000)}")

      println(s"2k $name 5 hits: ${countHits(m, keys, 5, 2000)}")
      println(s"50k $name 5 hits: ${countHits(m, keys, 5, 50000)}")
    }

    val zg = zdock.keySet intersect geo.keySet
    val gle = geo.keySet intersect elc.keySet intersect lip.keySet

    printHits(zdock, zg, "zdock")
    printHits(geo, zg, "zgeo")
    printHits(geo, gle, "geo")
    printHits(lip, gle, "lip")
    printHits(elc, gle, "elc")

    println(s"Success 2k: ${countSuccess(zdock, geo)}")

    def chart(prec: Double, count: Int) {
      val keys = geo.keySet intersect lip.keySet intersect elc.keySet
      println(s"Graph for prec $prec and count $count has ${keys.size} keys")
      val geoPoints = getSuccessPoints(geo, keys, prec, count).toXYSeries("геометрия")
      val lipPoints = getSuccessPoints(lip, keys, prec, count).toXYSeries("геометрия + гидрофобность")
      val elcPoints = getSuccessPoints(elc, keys, prec, count).toXYSeries("геометрия + электростатика")

      val chart = XYLineChart(List(geoPoints, lipPoints, elcPoints))
      chart.plot.getDomainAxis.setLabel("ограничение на число выдаваемых решений")
      chart.plot.getRangeAxis.setLabel("число комплексов с найденными решениями")
      chart.plot.getRenderer.setSeriesStroke(0, new BasicStroke(3))
      chart.plot.getRenderer.setSeriesStroke(1, new BasicStroke(3))
      chart.plot.getRenderer.setSeriesStroke(2, new BasicStroke(3))
      chart.show(resolution = (800, 600))
      chart.saveAsPNG(s"int${prec}_$count.png", (800, 600))
    }

    //    chart(2.5, 100000)
    //    chart(5, 100000)

    chart(2.5, 10000)
    //    chart(2.5, 1000000)
    chart(5, 10000)

    //    chart(2.5, 1000)
    //    chart(5, 1000)

    def chart2(prec: Double, count: Int) {
      val keys = geo.keySet intersect zdock.keySet

      val geoPoints = getSuccessPoints(geo, keys, prec, count).toXYSeries("предлагаемый подход")
      val zdockPoints = getSuccessPoints(zdock, keys, prec, count).toXYSeries("zdock")

      val chart = XYLineChart(List(geoPoints, zdockPoints))
      chart.plot.getDomainAxis.setLabel("ограничение на число выдаваемых решений")
      chart.plot.getRangeAxis.setLabel("число комплексов с найденными решениями")
      chart.plot.getRenderer.setSeriesStroke(0, new BasicStroke(3))
      chart.plot.getRenderer.setSeriesStroke(1, new BasicStroke(3))
      chart.show(resolution = (800, 600))
      chart.saveAsPNG(s"ext${prec}_$count.png", (800, 600))
    }

    //    chart2(2.5, 1000)
    //    chart2(5, 1000)
    //
    chart2(2.5, 50000)
    chart2(5, 50000)

    //    val reports = List("report_geometry.tsv", "report_lipophilic.tsv", "report_electric.tsv")
    //    val keys = Source.fromFile("/Users/vikharev/Documents/results/report_geometry.tsv").getLines().map(_.takeWhile(_ != '\t')).toList
    //
    //    val zd = Source.fromFile("/Users/vikharev/Documents/results/report_zdock.tsv").getLines().map(_.split("\t")).map(x => (x(0), x)).toMap
    //    val geo = Source.fromFile("/Users/vikharev/Documents/results/report_geometry.tsv").getLines().map(_.split("\t")).map(x => (x(0), x)).toMap
    //    val lip = Source.fromFile("/Users/vikharev/Documents/results/report_lipophilic.tsv").getLines().map(_.split("\t")).map(x => (x(0), x)).toMap
    //    val el = Source.fromFile("/Users/vikharev/Documents/results/report_electric.tsv").getLines().map(_.split("\t")).map(x => (x(0), x)).toMap
    //
    //    val zds = new scala.collection.mutable.ArrayBuffer[Int]
    //    val geos = new scala.collection.mutable.ArrayBuffer[Int]
    //
    //    var c = 0
    //    var t = 0
    //
    //    keys.foreach({
    //      key => {
    //        def esc(m: Map[String, Array[String]], i: Int): String = {
    //          if (m.contains(key)) m(key)(i) else "0"
    //        }
    //
    //        if (zd.contains(key) && !zd(key)(2).equals("computation time") && zd(key)(2).toInt != 0 && geo.contains(key) && !geo(key)(2).equals("computation time") && geo(key)(2).toInt != 0) {
    //          if (zd(key)(6).toInt == 0 && geo(key)(6).toInt != 0) {
    //            c += 1
    //          } else {
    //            t += 1
    //          }
    //        }
    //
    //
    //        //        println(s"$key & ${esc(zd, 2)} & ${esc(zd, 5)} & ${esc(zd, 6)} & ${esc(geo, 2)} & ${esc(geo, 5)} & ${esc(geo, 6)} \\\\ \\hline")
    //      }
    //    })
    //
    //    println(s"$c $t")
    //    println(s"${zds.sum / zds.size}")
    //    println(s"${geos.sum / geos.size}")

    //    reports.foreach({
    //      file => {
    //        val writer = new PrintWriter(s"/Users/vikharev/Documents/results/filtered_$file")
    //        val map = Source.fromFile(s"/Users/vikharev/Documents/results/$file")
    //          .getLines()
    //          .map(line => (line.takeWhile(_ != '\t'), line))
    //          .toMap
    //        keys.foreach(key => {
    //          if (map.contains(key)) {
    //            writer.write(map(key))
    //            writer.write("\n")
    //          } else {
    //            writer.write(s"$key\t-\t-\t-\t-\t-\t-\n")
    //          }
    //        })
    //
    //        writer.close()
    //      }
    //    })
  }

}
