package top.fengpingtech.solen.controller;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.fengpingtech.solen.auth.AuthService;
import top.fengpingtech.solen.model.Event;
import top.fengpingtech.solen.model.EventType;
import top.fengpingtech.solen.model.Tenant;
import top.fengpingtech.solen.service.AntMatchService;
import top.fengpingtech.solen.service.EventRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventRepository eventRepository;

    private final AntMatchService antMatchService;

    private final AuthService authService;

    public EventController(EventRepository eventRepository, AntMatchService antMatchService, AuthService authService) {
        this.eventRepository = eventRepository;
        this.antMatchService = antMatchService;
        this.authService = authService;
    }

    @RequestMapping("/event/list")
    public List<Event> list(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @RequestParam(value = "startTime", required = false) Date startTime,
            @RequestParam(value = "endTime", required = false) Date endTime,
            @RequestParam(value = "deviceId", required = false) String deviceId,
            @RequestParam(value = "startId", required = false) String startId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        Tenant tenant = authService.getTenant(appKey);
        List<String> patterns;
        if (deviceId != null && !deviceId.isEmpty()) {
            patterns = new ArrayList<>();
            for (String id : deviceId.trim().split("[,| ]+")) {
                if (antMatchService.antMatch(tenant.getDevicePatterns(), id)) {
                    patterns.add(id);
                }
            }
            if (patterns.isEmpty()) {
                patterns = tenant.getDevicePatterns();
            }
        } else if (!tenant.getRoles().contains(AuthService.ROLE_ADMIN)) {
            patterns = tenant.getDevicePatterns();
        } else {
            patterns = null;
        }

        int start = (pageNo - 1) * pageSize;

        List<EventType> eventTypes;
        if (type == null || type.isEmpty()) {
            eventTypes = null;
        } else {
            eventTypes = new ArrayList<>();
            for (String t : type.trim().split("[,|]")) {
                eventTypes.add(EventType.valueOf(t));
            }
        }

        return eventRepository.find(startTime, endTime, patterns,
                startId == null ? null : Long.parseLong(startId), eventTypes, start, pageSize);
    }
}
