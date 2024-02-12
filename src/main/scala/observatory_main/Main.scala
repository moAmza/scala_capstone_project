package observatory_main

import com.sksamuel.scrimage.implicits.*
import scala.util.Properties.isWin
import java.io.File
import com.sksamuel.scrimage.nio.PngWriter

object Main extends App:

  case class Data(localAverage: Array[(Location, Temperature)], grid: GridLocation => Temperature)

  def generateLocalAverage(year: Int, csvPath: String, stationPath: String) =
    val localDate = Extraction.locateTemperaturesRDD(
      year,
      Extraction.getStation(stationPath).persist,
      Extraction.getCsv(csvPath)
    )

    val localAverage = Extraction.locationYearlyAverageRecordsRDD(localDate).collect()
    println("Creating grid for year: " + year)
    val grid = Manipulation.makeGrid(localAverage)
    println("- Grid created")
    (year, Data(localAverage, grid))

  val colors = Array(
    (60.0, Color(255, 255, 255)),
    (32.0, Color(255, 0, 0)),
    (12.0, Color(255, 255, 0)),
    (0.0, Color(0, 255, 255)),
    (-15.0, Color(0, 0, 255)),
    (-27.0, Color(255, 0, 255)),
    (-50.0, Color(33, 0, 107)),
    (-60.0, Color(0, 0, 0)),
  )



  def generateImage(year: Year, tile: Tile, data: Data) =
    println(s"Creating tile: ...      ./target/temperatures/${year}/${tile.zoom}/${tile.x}-${tile.y}.png")
    val img = Visualization2.visualizeGrid(data.grid, colors, tile)
    println("- Tile created.")
    img.output(PngWriter.NoCompression, new File(s"./target/temperatures/${year}/${tile.zoom}/${tile.x}-${tile.y}.png"));


  Interaction.generateTiles(
    (1975 to 2015).map(i => generateLocalAverage(i, f"/${i}.csv", "/stations.csv")).toArray,
    generateImage
  )

  //  val localAverage = generateLocalAverage(2015, "/2015.csv", "/stations.csv")
//
//  val grid = Manipulation.makeGrid(localAverage._2.localAverage)
//  val img = Visualization2.visualizeGrid(grid, colors, Tile(0,0,0))
//  img.output(PngWriter.NoCompression, new File(s"./visualize.png"));
//
//  val img2 = Interaction.tile(localAverage._2.localAverage, colors, Tile(0, 0, 0))
//  img2.output(PngWriter.NoCompression, new File(s"./visualize2.png"));
//
//
//  val temp = Visualization.predictTemperature(localAverage._2.localAverage, Location(-85, 178))
//  println(temp)
//  //

//  val colorsDiff = Array(
//    (7.0, Color(0, 0, 0)),
//    (4.0, Color(255, 0, 0)),
//    (2.0, Color(255, 255, 0)),
//    (0.0, Color(255, 255, 255)),
//    (-2.0, Color(0, 255, 255)),
//    (-7.0, Color(0, 0, 255)),
//  )

  SparkObj.spark.close()


  if (isWin) System.setProperty("hadoop.home.dir", System.getProperty("user.dir") + "\\winutils\\hadoop-3.3.1")
end Main

