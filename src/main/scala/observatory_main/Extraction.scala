package observatory

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Dataset

import java.time.LocalDate
import scala3encoders.given
import org.apache.spark.sql.functions.*
import org.apache.spark.sql.DataFrameNaFunctions

/**
 * 1st milestone: data extraction
 */
object Extraction extends ExtractionInterface :

  def getStation (stationsFile: String) =
    SparkObj.readCSV (stationsFile)
      .toDF ("STN_s", "WBAN_s", "lat", "lon")
      .na.fill("-1", Seq("STN_s", "WBAN_s"))
      .where (col ("lat").isNotNull && col ("lon").isNotNull)
      .as[StationData]

  def getCsv (temperaturesFile: String) =
    SparkObj.readCSV (temperaturesFile)
      .toDF ("STN_c", "WBAN_c", "month", "day", "temp")
      .na.fill("-1", Seq("STN_c", "WBAN_c"))
      .as[TemperatureData]
  
  def toCelsius(x: Double) =
    (x - 32) * 5 / 9

  def locateTemperaturesRDD(
     year: Year,
     stationsFileRDD:  Dataset[StationData],
     temperaturesFileRDD:  Dataset[TemperatureData]
    ): RDD[(LocalDate, Location, Temperature)] =
    temperaturesFileRDD
        .joinWith(
          stationsFileRDD,
          (col("STN_c") === col("STN_s")) and (col("WBAN_c") === col("WBAN_s"))
        ).rdd
        .map(data => (
          LocalDate.of(year, data._1.month.toInt, data._1.day.toInt),
          Location(data._2.lat.toDouble, data._2.lon.toDouble),
          toCelsius(data._1.temp.toDouble)
        ))

  /** @param year
   * Year number
   * @param stationsFile
   * Path of the stations resource file to use (e.g. "/stations.csv")
   * @param temperaturesFile
   * Path of the temperatures resource file to use (e.g. "/1975.csv")
   * @return
   * A sequence containing triplets (date, location, temperature)
   */
  def locateTemperatures (
    year: Year,
    stationsFile: String,
    temperaturesFile: String
    ): Iterable[(LocalDate, Location, Temperature)] =
      locateTemperaturesRDD(
        year,
        getStation(stationsFile),
        getCsv(temperaturesFile)
      ).collect()

  def locationYearlyAverageRecordsRDD (records: RDD[(LocalDate, Location, Temperature)] ): RDD[(Location, Temperature)] =
    records.map(x => (x._2, x._3)).groupByKey.mapValues(x => x.sum / x.size)

  /**
   * @param records A sequence containing triplets (date, location, temperature)
   * @return A sequence containing, for each location, the average temperature over the year.
   */
  def locationYearlyAverageRecords (records: Iterable[(LocalDate, Location, Temperature)] ): Iterable[(Location, Temperature)] =
    locationYearlyAverageRecordsRDD(SparkObj.getRDD(records.toSeq)).collect()

case class StationData(
                        STN_s: String,
                        WBAN_s: String,
                        lat: String,
                        lon: String
                      )

case class TemperatureData(
                            STN_c: String,
                            WBAN_c: String,
                            month: String,
                            day: String,
                            temp: String
                          )
