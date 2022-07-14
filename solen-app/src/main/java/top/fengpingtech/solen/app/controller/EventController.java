package top.fengpingtech.solen.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.fengpingtech.solen.app.auth.AuthService;
import top.fengpingtech.solen.app.controller.bean.EventBean;
import top.fengpingtech.solen.app.controller.bean.EventQueryRequest;
import top.fengpingtech.solen.app.domain.EventDomain;
import top.fengpingtech.solen.app.mapper.EventMapper;
import top.fengpingtech.solen.app.repository.EventRepository;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventRepository eventRepository;

    private final AuthService authService;

    private final EventMapper eventMapper;

    public EventController(EventRepository eventRepository, AuthService authService, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.authService = authService;
        this.eventMapper = eventMapper;
    }

    @GetMapping("/event/list")
    public List<EventBean> list(EventQueryRequest request) {
        if (request.getPageNo() == null) {
            request.setPageNo(1);
        }

        if (request.getPageSize() == null) {
            request.setPageSize(100);
        }

        if (request.getStartTime() == null) {
            Date fiveMinAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
            request.setStartTime(fiveMinAgo);
        }

        PageRequest page = PageRequest.of(request.getPageNo() - 1, request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "eventId"));
        Specification<EventDomain> spec = (root, cq, cb) -> {
            List<Predicate> list = new ArrayList<>();
            if (request.getStartTime() != null) {
                list.add(cb.greaterThanOrEqualTo(root.get("time"), request.getStartTime()));
            }

            if (request.getEndTime() != null) {
                list.add(cb.lessThan(root.get("time"), request.getEndTime()));
            }

            if (request.getDeviceId() != null) {
                list.add(root.get("device").get("deviceId").in(Arrays.asList(request.getDeviceId().split("[, |]"))));
            }

            if (request.getStartId() != null) {
                list.add(cb.greaterThanOrEqualTo(root.get("eventId"), request.getStartId()));
            }

            if (request.getType() != null) {
                list.add(root.get("type").in(Arrays.asList(
                        request.getType().split("[, |]"))));
            }

            authService.fillAuthPredicate(root.get("device").get("deviceId"), cb, list);

            return cb.and(list.toArray(new Predicate[0]));
        };

        Page<EventDomain> events = eventRepository.findAll(spec, page);
        return eventMapper.mapToBean(events.getContent());
    }
}
