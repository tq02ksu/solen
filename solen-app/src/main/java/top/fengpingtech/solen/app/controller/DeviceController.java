package top.fengpingtech.solen.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
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
import top.fengpingtech.solen.app.repository.DeviceRepository;
import top.fengpingtech.solen.app.service.CoordinateTransformationService;
import top.fengpingtech.solen.server.DeviceService;
import top.fengpingtech.solen.server.SolenServer;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DeviceController {
    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    private final DeviceRepository deviceRepository;

    private final AuthService authService;

    private final CoordinateTransformationService coordinateTransformationService;

    public DeviceController(SolenServer solenServer, DeviceRepository deviceRepository,
                            AuthService authService, CoordinateTransformationService coordinateTransformationService) {
        this.deviceService = solenServer.getDeviceService();
        this.deviceRepository = deviceRepository;
        this.authService = authService;
        this.coordinateTransformationService = coordinateTransformationService;
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

        Specification<Device> spec = (root, cq, cb) -> {
            List<Predicate> list = new ArrayList<>();

            if (request.getDeviceId() != null) {
                list.add(root.get("deviceId").in(Arrays.asList(
                        request.getDeviceId().split("[, |]"))));
            }

            if (request.getStatus() != null) {
                list.add(root.get("status").in(Arrays.asList(request.getStatus().split("[, |]"))));
            }

            authService.fillAuthPredicate(root.get("deviceId"), cb, list);

            return cb.and(list.toArray(new Predicate[0]));
        };

        Page<Device> list = deviceRepository.findAll(spec, page);
        return PageableResponse.<DeviceBean>builder()
                .total(list.getTotalElements())
                .data(list.stream()
                        .map(model -> DeviceBean.builder()

                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/device/{deviceId}")
    public DeviceBean detail(@PathVariable ("deviceId") Long deviceId) {
        authService.checkAuth(deviceId);
        Optional<Device> device = deviceRepository.findById(deviceId);
        return DeviceBean.builder().build();
    }

    @DeleteMapping("/device/{deviceId}")
    public Object delete(
            @PathVariable("deviceId") Long deviceId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        authService.checkAuth(deviceId);
        Optional<Device> device = deviceRepository.findById(deviceId);
        deviceRepository.deleteById(deviceId);

        return ResponseEntity.ok(buildBean(device.get()));
    }


    @RequestMapping("/statByField")
    public Map<String, Long> statByField(@RequestParam String field) {
        return null;
    }

    @PostMapping("/sendControl")
    public DeviceBean sendControl(@RequestBody SendRequest request)
            throws ExecutionException, InterruptedException {
        authService.checkAuth(request.getDeviceId());

        deviceService.sendControl(String.valueOf(request.getDeviceId()), request.getCtrl());

        Optional<Device> device = deviceRepository.findById(request.getDeviceId());

        return buildBean(device.orElseThrow(IllegalArgumentException::new));
    }

    @PostMapping("/sendAscii")
    public DeviceBean sendAscii(
            @RequestBody SendRequest request) throws Exception {
        if (request.getData() == null) {
            throw new IllegalArgumentException("data can not be null");
        }

        if (request.getDeviceId() == null) {
            throw new IllegalArgumentException("deviceId can not be null");
        }

        authService.checkAuth(request.getDeviceId());

        deviceService.sendMessage(String.valueOf(request.getDeviceId()), request.getData());

        Optional<Device> device = deviceRepository.findById(request.getDeviceId());

        return buildBean(device.orElseThrow(IllegalArgumentException::new));
    }

    private DeviceBean buildBean(Device device) {
        DeviceBean bean = DeviceBean.builder()

                .build();

        if (device.getCoordinate() != null) {
            bean.setCoordinates(Arrays.asList(
                    device.getCoordinate(),
                    coordinateTransformationService.wgs84ToBd09(device.getCoordinate()),
                    coordinateTransformationService.wgs84ToGcj02(device.getCoordinate())
            ));
        }
        return bean;
    }
}
