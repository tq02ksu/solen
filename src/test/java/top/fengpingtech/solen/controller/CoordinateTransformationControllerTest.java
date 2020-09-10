package top.fengpingtech.solen.controller;

import org.junit.Test;
import top.fengpingtech.solen.bean.Coordinate;

public class CoordinateTransformationControllerTest {
    private CoordinateTransformationController controller = new CoordinateTransformationController();

    @Test
    public void gcj02ToBd09() {
    }

    @Test
    public void bd09ToGcj02() {
    }

    @Test
    public void wgs84ToGcj02() {
        Coordinate c = Coordinate.builder().lat(40.113693).lng(116.34478).build();

        Coordinate result = controller.wgs84ToGcj02(c);
        System.out.println(result);
    }

    @Test
    public void gcj02ToWgs84() {
    }

    @Test
    public void bd09ToWgs84() {
    }

    @Test
    public void wgs84ToBd09() {
        Coordinate c = Coordinate.builder().lat(40.113693).lng(116.34478).build();

        Coordinate result = controller.wgs84ToBd09(c);
        System.out.println(result);
    }
}
