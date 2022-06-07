package top.fengpingtech.solen.app.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.fengpingtech.solen.app.domain.support.MapConverter;
import top.fengpingtech.solen.server.model.EventType;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "event")
public class EventDomain {
    @Id
    @GeneratedValue
    private Long eventId;

    @ManyToOne
    @JoinColumn(name = "deviceId")
    private DeviceDomain device;

    @Enumerated
    private EventType type;

    @Temporal(TemporalType.TIMESTAMP)
    private Date time;

    @Convert(converter = MapConverter.class)
    private Map<String, String> details;
}
