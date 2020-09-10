package top.fengpingtech.solen.service;

import org.springframework.stereotype.Service;
import top.fengpingtech.solen.bean.Coordinate;
import top.fengpingtech.solen.bean.CoordinateSystem;

/**
 * 坐标转换
 * ref: http://www.openluat.com/GPS-Offset.html
 */
@Service
public class CoordinateTransformationService {

    private final double xPi = 3.14159265358979324 * 3000.0 / 180.0;
    private final double pi = 3.1415926535897932384626;  // π
    private final double a = 6378245.0; // 长半轴
    private final double ee = 0.00669342162296594323; // 扁率

    /**
     * 火星坐标系(GCJ-02)转百度坐标系(BD-09)
     * 谷歌、高德——>百度
     *
     * @param c coordinate to be process
     * @return coordinate transformed
     */
    public Coordinate gcj02ToBd09( Coordinate c) {
        double lng = c.getLng(), lat = c.getLat();
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * xPi);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * xPi);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return Coordinate.builder().system(CoordinateSystem.bd09).lat(bdLat).lng(bdLng).build();
    }

    /**
     * 百度坐标系(BD-09)转火星坐标系(GCJ-02)
     * 百度——>谷歌、高德
     *
     * @param c coordinate to be process
     * @return coordinate transformed
     */
    public Coordinate bd09ToGcj02(Coordinate c) {
        double bdLat = c.getLat(), bdLng = c.getLng();
        double x = bdLng - 0.0065;
        double y = bdLat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * xPi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * xPi);
        double lng = z * Math.cos(theta);
        double lat = z * Math.sin(theta);
        return Coordinate.builder().system(CoordinateSystem.gcj02).lng(lng).lat(lat).build();
    }

    /**
     * WGS84转GCJ02(火星坐标系)
     *
     * @param c coordinate to be process
     * @return coordinate transformed
     */
    public Coordinate wgs84ToGcj02(Coordinate c) {
        if (outOfChina(c.getLng(), c.getLat())) {
            return c;
        }

        double dLat = transformLat(c.getLng() - 105.0, c.getLat() - 35.0);
        double dLng = transformLng(c.getLng() - 105.0, c.getLat() - 25.0);
        double radLat = c.getLat() / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = c.getLat() + dLat;
        double mgLng = c.getLng() + dLng;
        return Coordinate.builder().system(CoordinateSystem.gcj02).lat(mgLat).lng(mgLng).build();
    }

    /**
     * GCJ02(火星坐标系)转GPS84
     * 	:param lng:火星坐标系的经度
     * 	:param lat:火星坐标系纬度
     */
    public Coordinate gcj02ToWgs84( Coordinate c) {
        if (outOfChina(c.getLng(), c.getLat())) {
            return c;
        }
        double dLat = transformLat(c.getLng() - 105.0, c.getLat() - 35.0);
        double dLng = transformLng(c.getLng() - 105.0, c.getLat() - 35.0);
        double radLat = c.getLat() / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = c.getLat() + dLat;
        double mgLng = c.getLng() + dLng;
        return Coordinate.builder()
                .system(CoordinateSystem.wgs84)
                .lng(c.getLng() * 2 - mgLng)
                .lat(c.getLat() * 2 - mgLat).build();
    }

    public Coordinate bd09ToWgs84( Coordinate c) {
        return gcj02ToWgs84(bd09ToGcj02(c));
    }

    public Coordinate wgs84ToBd09(Coordinate c) {
        return gcj02ToBd09(wgs84ToGcj02(c));
    }

    /**
     * 判断是否在国内，不在国内不做偏移
     */
    private boolean outOfChina(double lng, double lat) {
        return !(lng > 73.66 && lng < 135.05 && lat > 3.86 && lat < 53.55);
    }

    private double transformLat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat +
                0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * pi) + 20.0 *
                Math.sin(2.0 * lng * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * pi) + 40.0 *
                Math.sin(lat / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * pi) + 320 *
                Math.sin(lat * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng +
                0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * pi) + 20.0 *
                Math.sin(2.0 * lng * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * pi) + 40.0 *
                Math.sin(lng / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * pi) + 300.0 *
                Math.sin(lng / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }
}
