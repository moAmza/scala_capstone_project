package observatory
import observatory_main.CellPoint

trait Visualization2Test extends MilestoneSuite:
  private val milestoneTest = namedMilestoneTest("value-added information visualization", 5) _

  // Implement tests for methods of the `Visualization2` object

  test("bilinear interpolation") {
    assertEquals(observatory_main.Visualization2.bilinearInterpolation(CellPoint(0,0), 0,0,0,0), 0.0)
  }


  test("bilinear interpolation: test 2") {
    assertEquals(observatory_main.Visualization2.bilinearInterpolation(CellPoint(0, 0.1), 0, 0, 0, -1.8777056010916598E-5), 0E-23)
  }

  test("bilinear interpolation: test 3") {
    assertEquals(observatory_main.Visualization2.bilinearInterpolation(CellPoint(0.1, 0), -1.52587890625E-5, 0, 0, 0), -0.000013732910156250)
  }
