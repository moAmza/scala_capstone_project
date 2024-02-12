package observatory

import com.sksamuel.scrimage.pixels.Pixel
import observatory_main.{Location, Color}

trait VisualizationTest extends MilestoneSuite:
  private val milestoneTest = namedMilestoneTest("raw data display", 2) _

  // Implement tests for the methods of the `Visualization` object
  test("interpolateColor: exceeding the greatest value of a color scale should return the color associated with the greatest value") {
    assertEquals(
      observatory_main.Visualization.interpolateColor(
        List((0.0,Color(255,0,0)), (1.977249004553591E-5,Color(0,0,255))),
        value = 0.0
      ),
      Color(255,0,0)
    )
  }

  test("Incorrect predicted color: Color(0,0,255). Expected: Color(191,0,64)") {
    assertEquals(
      observatory_main.Visualization.interpolateColor(
        List((-2.147483648E9,Color(255,0,0)), (0.0,Color(0,0,255))),
        -1.610612736E9
      ),
      Color(191,0,64)
    )
  }


  test("Circular distance: my test 1") {
    assertEquals(
      observatory_main.Visualization.circularDistance(Location(1, 1), Location(1, 2)),
      111301.8901918003
    )
  }

  test("Circular distance: my test 2") {
    assertEquals(
      observatory_main.Visualization.circularDistance(Location(1, 1), Location(10, 5)),
      1095342.573920593
    )
  }

  test("Predict Temperature: my test 1") {
    assertEquals(
      observatory_main.Visualization.predictTemperature(
        Array(
          (Location(10, 5), 12.0),
          (Location(1, 2), -50.0),
        ),
        Location(1, 1)
      )
      , -49.366370222076824
    )
  }


//  test("Visualize test: my test 1") {
//    assertEquals(
//      Visualization.visualize(
//        Array(
//          (Location(10,5), 0.0),
//          (Location(15,3),60.0),
//          (Location(70, 70), -50.0)
//        ),
//        Array(
//          (60.0, Color(255, 0, 0)),
//          (0.0, Color(0, 255, 0)),
//          (-60.0, Color(0, 0, 255))
//        )
//      ).pixels().take(3).map(x => x.argb),
//      Array[Int](
//        -16717037,
//        -16716527,
//        -16716017
//      )
//    )
//  }

  override def afterAll() =
    observatory_main.SparkObj.spark.close()
