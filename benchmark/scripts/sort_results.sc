import java.io.PrintWriter
import scala.io.Source

val reports = List("report_zdock.tsv", "report_geometry.tsv", "report_lipophilic.tsv", "report_electric.tsv")
val keys = Source.fromFile("/Users/vikharev/Documents/results/report_zdock.tsv").getLines().map(_.takeWhile(_ != '\t')).toList
reports.foreach({
  file => {
    val writer = new PrintWriter(s"/Users/vikharev/Documents/results/filtered_$file")
    val map = Source.fromFile(s"/Users/vikharev/Documents/results/$file")
      .getLines()
      .map(line => (line.takeWhile(_ != '\t'), line))
      .toMap
    keys.foreach(key => {
      if (map.contains(key)) {
        writer.write(map(key))
        writer.write("\n")
      } else {
        writer.write(s"$key\t-\t-\t-\t-\t-\t-\n")
      }
    })

    writer.close()
  }
})