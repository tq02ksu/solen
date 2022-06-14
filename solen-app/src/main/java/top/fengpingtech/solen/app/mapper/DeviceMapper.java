package top.fengpingtech.solen.app.mapper;

import org.mapstruct.*;
import top.fengpingtech.solen.app.controller.bean.DeviceBean;
import top.fengpingtech.solen.app.domain.Coordinate;
import top.fengpingtech.solen.app.domain.CoordinateSystem;
import top.fengpingtech.solen.app.domain.DeviceDomain;
import top.fengpingtech.solen.app.domain.EventDomain;
import top.fengpingtech.solen.app.service.CoordinateTransformationService;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeviceMapper {
    @Mapping(target = "coordinates", expression = "java(getCoordinates(domain))")
    DeviceBean mapToBean(DeviceDomain domain, List<EventDomain> events);

    @Named(value = "mapToBean4List")
    @Mapping(target = "coordinates", expression = "java(getCoordinates(domain))")
    DeviceBean mapToBeanSummary(DeviceDomain domain);

    @IterableMapping(qualifiedByName = "mapToBean4List")
    List<DeviceBean> mapToBean(List<DeviceDomain> domain);

    default List<Coordinate> getCoordinates(DeviceDomain domain) {
        if (domain.getLng() == null || domain.getLat() == null) {
            return null;
        }
        Coordinate coordinate = new Coordinate(CoordinateSystem.WGS84, domain.getLng(), domain.getLat());
        CoordinateTransformationService transformer = new CoordinateTransformationService();
        return Arrays.asList(
                coordinate,
                transformer.wgs84ToBd09(coordinate),
                transformer.wgs84ToGcj02(coordinate));
    }
}
