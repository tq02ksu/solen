package top.fengpingtech.solen.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.fengpingtech.solen.app.controller.bean.EventBean;
import top.fengpingtech.solen.app.domain.EventDomain;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {
    @Mapping(target = "deviceId", source = "event.device.deviceId")
    EventBean mapToBean(EventDomain event);

    List<EventBean> mapToBean(List<EventDomain> event);
}
