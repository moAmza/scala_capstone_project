package observatory_main

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.implicits.given
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.pixels.Pixel
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Column, Dataset}

import java.io.File
import scala.collection.parallel.CollectionConverters.given
import math.Ordering.Double

import scala3encoders.given
import org.apache.spark.sql.functions.*
import SparkObj.spark.implicits.*


/**
  * 2nd milestone: basic visualization
  */
object Visualization extends VisualizationInterface:
  val pValue = 6
  def circularDistance(loc1: Location, loc2: Location) =
    import Math.*
    abs(
      acos(
        sin(toRadians(loc1.lat)) * sin(toRadians(loc2.lat)) +
          cos(toRadians(loc1.lat)) * cos(toRadians(loc2.lat)) * cos(abs(toRadians(loc1.lon-loc2.lon)))
      )
    ) * 6.3781 * pow(10,6)

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Temperature)], location: Location): Temperature =
    val sum = temperatures.foldLeft((0.0, 0.0))((sum, temp) =>
      val dist = circularDistance(location, temp._1)
      if dist < 1000 then return temp._2
      else
        val w = 1 / Math.pow(dist, pValue)
        (sum._1 + w * temp._2, sum._2 + w)
    )

    sum._1 / sum._2

  def findTemperaturesRange(points: Iterable[(Temperature, Color)], value: Temperature) =
    points.foldLeft[(Option[(Double, Color)], Option[(Double, Color)])](Option.empty, Option.empty)((res, temp) =>
      if temp._1 > value then
        res._2 match
          case None => (res._1, Some(temp))
          case Some(result) => if temp._1 < result._1 then (res._1, Some(temp)) else res
      else
        res._1 match
          case None => (Some(temp), res._2)
          case Some(result) => if temp._1 > result._1 then (Some(temp), res._2) else res
    )

  /**
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Temperature, Color)], value: Temperature): Color =
    def getIntermediateColor(min: (Temperature, Color), max: (Temperature, Color)) =
      val a = (value - min._1) / (max._1 - min._1)
      Color(
        Math.round(a * (max._2.red - min._2.red) + min._2.red).toInt,
        Math.round(a * (max._2.green - min._2.green) + min._2.green).toInt,
        Math.round(a * (max._2.blue - min._2.blue) + min._2.blue).toInt,
      )

    findTemperaturesRange(points, value) match
      case (Some(min), None) => min._2
      case (None, Some(max)) => max._2
      case (Some(min), Some(max)) => getIntermediateColor(min,max)
      case (None, None) => Color(0,0,0)


  def getColorsGenerator(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)])(location: Location) =
    val temp = predictTemperature(temperatures, location)
    println((location, temp))
    interpolateColor(colors, temp)


  def getImageFromLocations(width: Int, height: Int)(getColorFunc: (Location) => Color, alpha: Int = 255)(getLocationFunc: (Int) => Location) =
    def getPixel(width: Int, height: Int)(indexedColor: (Color, Long)) =
      val i = indexedColor._2.toInt
      val color = indexedColor._1
      val x = (i % width)
      val y = (i - x) / width
      Pixel(x, y, color.red, color.green, color.blue, alpha)

    val pixels = SparkObj.getRDD((0 until width * height))
      .map(getLocationFunc)
      .map(getColorFunc)
      .zipWithIndex
      .collect()
      .map(getPixel(width, height))
    ImmutableImage.create(width, height, pixels)


  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)]): ImmutableImage =
    def getLocationFunc(i: Int) =
      println(i)
      Location(90 - (i - (i % 360)) / 360, (i % 360) - 180)
    val getColorFunc = getColorsGenerator(temperatures, colors)
    getImageFromLocations(360, 180)(getColorFunc)(getLocationFunc)

