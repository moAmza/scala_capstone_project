package observatory_main

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata

/**
  * 5th milestone: value-added information visualization
  */
object Visualization2 extends Visualization2Interface:
  /**
    * @param point (x, y) coordinates of a point in the grid cell
    * @param d00 Top-left value
    * @param d01 Bottom-left value
    * @param d10 Top-right value
    * @param d11 Bottom-right value
    * @return A guess of the value at (x, y) based on the four known values, using bilinear interpolation
    *         See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
    */
  def bilinearInterpolation(
    point: CellPoint,
    d00: Temperature,
    d01: Temperature,
    d10: Temperature,
    d11: Temperature
  ): Temperature =
    val x0 = d00 + point.x * (d10 - d00)
    val x1 = d01 + point.x * (d11 - d01)
    x0 + (x1 - x0) * point.y


  /**
    * @param grid Grid to visualize
    * @param colors Color scale to use
    * @param tile Tile coordinates to visualize
    * @return The image of the tile at (x, y, zoom) showing the grid using the given color scale
    */
  def visualizeGrid(
    grid: GridLocation => Temperature,
    colors: Iterable[(Temperature, Color)],
    tile: Tile
  ): ImmutableImage =
    import math.*
    def getColorFunc(loc: Location) =
      val (lat0, lat1) = (floor(loc.lat).toInt, if ceil(loc.lat).toInt == 180 then -180 else ceil(loc.lat).toInt)
      val (lon0, lon1) = (floor(loc.lon).toInt, if ceil(loc.lon).toInt == 180 then -180 else ceil(loc.lon).toInt)
      val cellPoint = CellPoint(loc.lon - lon0, lat1 - loc.lat)
      val (d00, d01, d10, d11) = (
        grid(GridLocation(lat1, lon0)),
        grid(GridLocation(lat0, lon0)),
        grid(GridLocation(lat1, lon1)),
        grid(GridLocation(lat0, lon1))
      )
      val temp = bilinearInterpolation(cellPoint, d00, d01, d10, d11)
      val color = Visualization.interpolateColor(colors, temp)
      color

    Visualization.getImageFromLocations(256,256)(getColorFunc, 127)(Interaction.getLocationFunc(tile))
