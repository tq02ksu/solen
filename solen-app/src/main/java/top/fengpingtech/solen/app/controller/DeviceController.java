package top.fengpingtech.solen.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.fengpingtech.solen.app.auth.AuthService;
import top.fengpingtech.solen.app.controller.bean.DeviceBean;
import top.fengpingtech.solen.app.controller.bean.DeviceQueryRequest;
import top.fengpingtech.solen.app.controller.bean.PageableResponse;
import top.fengpingtech.solen.app.domain.DeviceDomain;
import top.fengpingtech.solen.app.domain.EventDomain;
import top.fengpingtech.solen.app.mapper.DeviceMapper;
import top.fengpingtech.solen.app.repository.DeviceRepository;
import top.fengpingtech.solen.app.repository.EventRepository;
import top.fengpingtech.solen.app.service.DeviceDomainService;
import top.fengpingtech.solen.server.DeviceService;
import top.fengpingtech.solen.server.model.EventType;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class DeviceController {
    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    private final DeviceDomainService deviceDomainService;

    private final AuthService authService;

    private final EventRepository eventRepository;

    private final DeviceMapper deviceMapper;

    public DeviceController(DeviceService deviceService, DeviceDomainService deviceDomainService,
                            AuthService authService, EventRepository eventRepository, DeviceMapper deviceMapper) {
        this.deviceService = deviceService;
        this.deviceDomainService = deviceDomainService;
        this.authService = authService;
        this.eventRepository = eventRepository;
        this.deviceMapper = deviceMapper;
    }

    @RequestMapping("/list")
    public PageableResponse<DeviceBean> list(DeviceQueryRequest request) {
        if (request.getPageNo() == null) {
            request.setPageNo(1);
        }

        if (request.getPageSize() == null) {
            request.setPageSize(100);
        }

        PageRequest page = PageRequest.of(request.getPageNo() - 1, request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "deviceId"));

        Specification<DeviceDomain> spec = (root, cq, cb) -> {
            List<Predicate> list = new ArrayList<>();

            if (request.getDeviceId() != null && !request.getDeviceId().isEmpty()) {
                list.add(root.get("deviceId").in(Arrays.asList(
                        request.getDeviceId().split("[, |]"))));
            }

            if (request.getStatus() != null) {
                list.add(root.get("status").in(Arrays.asList(request.getStatus().split("[, |]"))));
            }

            authService.fillAuthPredicate(root.get("deviceId"), cb, list);

            return cb.and(list.toArray(new Predicate[0]));
        };

        Page<DeviceDomain> list = deviceDomainService.findAll(spec, page);
        return PageableResponse.<DeviceBean>builder()
                .total(list.getTotalElements())
                .data(deviceMapper.mapToBean(list.getContent()))
                .build();
    }

    @GetMapping("/device/{deviceId}")
    public DeviceBean detail(@PathVariable ("deviceId") String deviceId) {
        Optional<DeviceDomain> device = deviceDomainService.findById(deviceId);

        if (!device.isPresent()) {
            throw new IllegalArgumentException("device not found!");
        }
        DeviceDomain domain = device.get();
        if (!authService.canVisit(domain)) {
            throw new IllegalArgumentException("can not visit the device");
        }

        Pageable pageable = PageRequest.of(0, 10, Sort.by("time").descending());
        List<EventDomain> events = eventRepository.findByDeviceAndType(domain, EventType.MESSAGE_RECEIVING, pageable);

        return deviceMapper.mapToBean(domain, events);
    }

    @DeleteMapping("/device/{deviceId}")
    public Object delete(
            @PathVariable("deviceId") String deviceId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        Optional<DeviceDomain> device = deviceDomainService.findById(deviceId);
        if (!device.isPresent()) {
            throw new IllegalArgumentException("device not found!");
        }
        DeviceDomain domain = device.get();
        if (!authService.canVisit(domain)) {
            throw new IllegalArgumentException("can not visit the device");
        }

        deviceDomainService.delete(domain);

        return deviceMapper.mapToBeanSummary(domain);
    }

    @RequestMapping("/statByField")
    public Map<String, Long> statByField(@RequestParam String field) {
        return null;
    }

    @PostMapping("/sendControl")
    public DeviceBean sendControl(@RequestBody SendRequest request) {
        Optional<DeviceDomain> device = deviceDomainService.findById(request.getDeviceId());
        if (!device.isPresent()) {
            throw new IllegalArgumentException("device not found!");
        }
        DeviceDomain domain = device.get();
        if (!authService.canVisit(domain)) {
            throw new IllegalArgumentException("can not visit the device");
        }

        deviceService.sendControl(String.valueOf(request.getDeviceId()), request.getCtrl());



        return deviceMapper.mapToBeanSummary(domain);
    }

    @PostMapping("/sendAscii")
    public DeviceBean sendAscii(
            @RequestBody SendRequest request) {
        if (request.getData() == null) {
            throw new IllegalArgumentException("data can not be null");
        }

        if (request.getDeviceId() == null) {
            throw new IllegalArgumentException("deviceId can not be null");
        }
        Optional<DeviceDomain> device = deviceDomainService.findById(request.getDeviceId());
        if (!device.isPresent()) {
            throw new IllegalArgumentException("device not found!");
        }
        DeviceDomain domain = device.get();
        if (!authService.canVisit(domain)) {
            throw new IllegalArgumentException("can not visit the device");
        }

        deviceService.sendMessage(request.getDeviceId(), request.getData());
        return deviceMapper.mapToBeanSummary(domain);
    }
}
