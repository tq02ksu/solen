package top.fengpingtech.solen.app.service;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import top.fengpingtech.solen.app.config.SolenServerProperties;
import top.fengpingtech.solen.app.repository.EventRepository;

import javax.annotation.PostConstruct;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class EventCleaner {
    private final TransactionTemplate transactionTemplate;

    private final SolenServerProperties serverProperties;

    private final EventRepository eventRepository;

    private final ScheduledExecutorService executorService;

    public EventCleaner(TransactionTemplate transactionTemplate, SolenServerProperties serverProperties,
                        EventRepository eventRepository) {
        this.transactionTemplate = transactionTemplate;
        this.serverProperties = serverProperties;
        this.eventRepository = eventRepository;
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
        threadFactory.setDaemon(true);
        threadFactory.setThreadNamePrefix("event-cleaner-");
        executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @PostConstruct
    public void init() {
        int initDelay = new Random().nextInt(600);
        executorService.scheduleWithFixedDelay(this::doClean, initDelay, 600, TimeUnit.SECONDS);
    }

    private void doClean() {
        transactionTemplate.execute(action -> {
            Duration retention = serverProperties.getEventRetention();
            LocalDateTime now = LocalDateTime.now();
            ZoneId zoneId = ZoneId.systemDefault();
            Instant instant = now.minus(retention).atZone(zoneId).toInstant();
            Date date = Date.from(instant);
            eventRepository.deleteByTimeLessThan(date);
            return null;
        });
    }
}
