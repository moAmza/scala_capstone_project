package observatory_main

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import scala.reflect.ClassTag
import scala.io.Source

object SparkObj {
  Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
  val spark =
    SparkSession
      .builder()
      .appName("observatory")
      .master("local")
      .getOrCreate()

  import spark.sqlContext.implicits._

  def readCSV(path: String): DataFrame =
    val lines =
      Source.fromInputStream(getClass.getResourceAsStream(path), "utf-8")
        .getLines()
        .toList
    val dataset = spark.sparkContext.parallelize[String](lines)
      .flatMap(x => x.split("'"))
      .toDS()
    spark.read.csv(dataset)

  def getRDD[T:ClassTag](seq: Seq[T]) = spark.sparkContext.parallelize(seq)
}

