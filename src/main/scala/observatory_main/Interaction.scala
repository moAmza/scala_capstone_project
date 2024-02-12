package observatory_main

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata
import org.apache.spark.sql.Column

import scala3encoders.given
import org.apache.spark.sql.functions.*
import SparkObj.spark.implicits.*

/**
  * 3rd milestone: interactive visualization
  */
object Interaction extends InteractionInterface:

  /**
    * @param tile Tile coordinates
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(tile: Tile): Location =
    import scala.math.*
    Location(
      toDegrees(atan(sinh(Pi * (1.0 - 2.0 * tile.y.toDouble / (1 << tile.zoom))))),
      tile.x.toDouble / (1 << tile.zoom) * 360.0 - 180.0,
    )

  def getLocationFunc(tile: Tile)(i: Int): Location =
    val x = 256 * tile.x + i % 256
    val y = 256 * tile.y + (i - i % 256) / 256
    tileLocation(Tile(x, y, tile.zoom + 8))


  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param tile Tile coordinates
    * @return A 256×256 image showing the contents of the given tile
    */
  def tile(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)], tile: Tile): ImmutableImage =
    val getColorFunc = Visualization.getColorsGenerator(temperatures, colors)
    Visualization.getImageFromLocations(256,256)(getColorFunc, 127)(getLocationFunc(tile))
//    val getColorFunc = Visualization.getFastColorsGenerator(temperatures, colors)
////    def getLocationFunc(i: Column): Location =
////      val x = i % 256 + 256 * tile.x
////      val y = (i - i % 256) / 256 + 256 * tile.y
////      tileLocation(Tile(x, y, tile.zoom + 8))
////
//    Visualization.fastGetImage(256, 256)(getColorFunc, 127)(Visualization.defaultLocationFunc)

/**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
 *
    * @param yearlyData Sequence of (year, data), where `data` is some data associated with
    *                   `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
  ): Unit =
    for
      z <- (0 to 3)
      i <- (0 until Math.pow(2,z).toInt)
      j <- (0 until Math.pow(2,z).toInt)
      (year, data) <- yearlyData
    do
      generateImage(year, Tile(i, j, z), data)
